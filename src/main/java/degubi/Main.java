package degubi;

import static degubi.Settings.*;
import static java.util.Spliterator.*;

import jakarta.json.*;
import jakarta.json.bind.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;
import javax.swing.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

public final class Main {
    public static final String VERSION = "1.3.0";
    public static final Jsonb json = JsonbBuilder.create(new JsonbConfig().withFormatting(Boolean.TRUE));

    public static void main(String[] args) throws Exception {
        var versionCheckResultMessage = CompletableFuture.supplyAsync(Main::getVersionCheckResultMessage);

        if(args.length == 0) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            var frame = new JFrame("PDF Table Extractor Settings");
            frame.setContentPane(Components.createSettingsPanel());
            frame.setSize(600, 630);
            frame.setLocationRelativeTo(null);
            frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Main.class.getResource("/app.png")));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);

            showVersionCheckResult(versionCheckResultMessage);
        }else{
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

            var rowComparisonFunction = getComparisonFunction(rowComparisonMethod, rowsPerPage);
            var columnComparisonFunction = getComparisonFunction(columnComparisonMethod, columnsPerPage);
            var pageNamingFunction = getPageNamingFunction(pageNamingMethod);
            var outputPathCreatorFunction = getOutputPathCreatorFunction(outputDirectoryMehod);
            var pdfSrc = parallelExtraction ? Arrays.stream(args).parallel()
                                            : Arrays.stream(args);
            pdfSrc.map(Path::of)
                  .map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .peek(k -> System.out.println("Opening file: " + k))
                  .filter(Main::checkIsPdfFile)
                  .forEach(inputFile -> handlePDFExtraction(inputFile, pageNamingFunction, outputPathCreatorFunction, rowComparisonFunction, columnComparisonFunction));

            showVersionCheckResult(versionCheckResultMessage);

            System.out.print("All done, press enter");
            System.console().readLine();
        }
    }

    private static void showVersionCheckResult(CompletableFuture<String> versionCheckResultMessage) {
        try {
            System.out.println(versionCheckResultMessage.get(15, TimeUnit.SECONDS));
        }catch(TimeoutException | InterruptedException | ExecutionException e) {
            System.out.println("Version checking timed out... :/");
        }
    }

    private static void handlePDFExtraction(String inputFile, PageNamingFunction pageNamingFunction, Function<Path, String> outputPathCreatorFunction,
                                            IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction) {

        System.out.println("Extracting data from: " + inputFile);

        try(var pdfInput = PDDocument.load(new File(inputFile));
            var excelOutput = extractPDF(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, pdfInput)) {

            var numberOfSheets = excelOutput.getNumberOfSheets();
            if(numberOfSheets > 0) {
                var outputFile = Path.of(outputPathCreatorFunction.apply(Path.of(inputFile)) + ".xlsx");

                System.out.println("Writing " + numberOfSheets + " sheets to file: " + outputFile);

                try(var outputStream = Files.newOutputStream(outputFile)){
                    excelOutput.write(outputStream);
                }

                System.out.println("Finished: " + outputFile + '\n');
            }else{
                System.out.println("No sheets were extracted from file: " + inputFile);
            }
        }catch(Exception e) {
            System.out.println("An error happened, check error.txt for details");

            try(var exceptionOutput = new PrintStream("error.txt")) {
                e.printStackTrace(exceptionOutput);
            }catch(FileNotFoundException e1) {}
        }
    }

    @SuppressWarnings({"unchecked", "cast"})
    public static XSSFWorkbook extractPDF(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                          PageNamingFunction pageNamingFunction, PDDocument pdfInput) {

        var textExtractor = new SpreadsheetExtractionAlgorithm();
        var rawPages = StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdfInput).extract(), ORDERED | IMMUTABLE), false)
                                    .map(textExtractor::extract)
                                    .toArray(List[]::new);

        var extractedPages = (List<Table>[]) rawPages;   //Gotta love Java's non generic arrays, this cast is fine
        var excelOutput = new XSSFWorkbook();

        IntStream.range(0, extractedPages.length)
                 .forEach(pageIndex -> {
                     var tables = extractedPages[pageIndex];

                     IntStream.range(0, tables.size())
                              .forEach(tableIndex -> writeTable(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction,
                                                                columnComparisonFunction, pageNamingFunction, excelOutput, pageIndex, tables, tableIndex));
                 });

        return excelOutput;
    }

    private static void writeTable(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                   PageNamingFunction pageNamingFunction, XSSFWorkbook excelOutput, int pageIndex, List<Table> tables, int tableIndex) {

        var rawRows = tables.get(tableIndex).getRows();

        if(rawRows.size() > 0) {
            var rows = rawRows.stream()
                              .map(k -> k.stream().map(RectangularTextContainer::getText).toArray(String[]::new))
                              .toArray(String[][]::new);

            var removableColumnCountFromBegin = (emptyColumnSkipMethod & 1) == 0 ? 0 : calculateRemovableColumnCount(rows, RowWalkerFunction.walkForwards());
            var removableColumnCountFromEnd = (emptyColumnSkipMethod & 2) == 0 ? 0 : calculateRemovableColumnCount(rows, RowWalkerFunction.walkBackwards());
            var removableRowCountFromBegin = (emptyRowSkipMethod & 1) == 0 ? 0 : calculateRemovableRowCount(rows, RowProviderFunction.providingForwards());
            var removableRowCountFromEnd = (emptyRowSkipMethod & 2) == 0 ? 0 : calculateRemovableRowCount(rows, RowProviderFunction.providingBackwards());

            if(rowComparisonFunction.test(rows.length - removableRowCountFromBegin - removableRowCountFromEnd) &&
               columnComparisonFunction.test(rows[0].length - removableColumnCountFromBegin - removableColumnCountFromEnd)) {

                var pageSheet = excelOutput.createSheet(pageNamingFunction.apply(excelOutput, pageIndex, tableIndex));

                IntStream.range(removableRowCountFromBegin, rows.length - removableRowCountFromEnd)
                         .forEach(rowIndex -> writeRow(rows, removableColumnCountFromBegin, removableColumnCountFromEnd, pageSheet, rowIndex));

                if(autosizeColumns) {
                    IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                             .forEach(pageSheet::autoSizeColumn);
                }

                pageSheet.setActiveCell(CellAddress.A1);
            }
        }
    }

    private static void writeRow(String[][] rows, int removableColumnCountFromBegin, int removableColumnCountFromEnd, XSSFSheet pageSheet, int rowIndex) {
        var excelRow = pageSheet.createRow(rowIndex);

        IntStream.range(removableColumnCountFromBegin, rows[rowIndex].length - removableColumnCountFromEnd)
                 .forEach(columnIndex -> excelRow.createCell(columnIndex).setCellValue(rows[rowIndex][columnIndex]));
    }

    private static boolean checkIsPdfFile(String filePath) {
        if(!filePath.endsWith(".pdf")) {
            System.out.println(filePath + " is not a pdf file");
            return false;
        }

        return true;
    }



    //This function walks rows forwards/backwards until it finds a non blank value
    //instead of walking columns downwards/upwards, avoiding jumping between arrays
    private static int calculateRemovableColumnCount(String[][] data, RowWalkerFunction walkerFunction) {
        return Arrays.stream(data)
                     .mapToInt(columnData -> countRemovableColumnsInRow(walkerFunction, columnData))
                     .min()
                     .orElse(0);
    }

    private static int countRemovableColumnsInRow(RowWalkerFunction walkerFunction, String[] columnData) {
        return IntStream.range(0, columnData.length)
                        .mapToObj(columnIndex -> walkerFunction.apply(columnData, columnIndex))
                        .takeWhile(String::isBlank)
                        .mapToInt(i -> 1)
                        .sum();
    }

    //This function walks rows forwards/backwards until it finds a row that is not fully empty
    private static int calculateRemovableRowCount(String[][] data, RowProviderFunction elementProvider) {
        return IntStream.range(0, data.length)
                        .takeWhile(k -> Arrays.stream(elementProvider.apply(data, k)).allMatch(String::isBlank))
                        .map(k -> 1)
                        .sum();
    }

    //0: Counting, 1: PageOrdinal+TableOrdinal
    public static PageNamingFunction getPageNamingFunction(int namingMethod) {
        return namingMethod == 0 ? (workbook, pageIndex, tableIndex) -> (workbook.getNumberOfSheets() + 1) + "."
                                 : (workbook, pageIndex, tableIndex) -> (pageIndex + 1) + ". Page " + (tableIndex + 1) + ". Table";
    }

    //0: Less/equal, 1: equal, 2: greater/equal
    public static IntPredicate getComparisonFunction(int comparisonMethod, int value) {
        return comparisonMethod == 0 ? k -> k <= value :
               comparisonMethod == 1 ? k -> k == value :
                                       k -> k >= value;
    }

    private static Function<Path, String> getOutputPathCreatorFunction(int outDirMethod) {
        return outDirMethod == 0 ? k -> stripExtension(k.toString()) :
               outDirMethod == 1 ? k -> Components.showExcelDirectoryPicker(k.getParent().toString()) + '\\' + stripExtension(k.getFileName().toString()) :
                                   k -> Settings.userSelectedOutputDirectory + '\\' + stripExtension(k.getFileName().toString());
    }

    private static String stripExtension(String path) {
        return path.substring(0, path.lastIndexOf('.'));
    }

    private static String getVersionCheckResultMessage() {
        if(versionCheckingDisabled) {
            return "Local app version: " + VERSION + ", version checking was disabled in config";
        }

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build();
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("https://api.github.com/repos/Degubi/PDFTableExtractor/releases/latest"))
                                 .build();
        try {
            var rawResponse = client.send(request, BodyHandlers.ofString()).body();
            var parsedResponse = json.fromJson(rawResponse, JsonObject.class);
            var remoteVersion = parsedResponse.getString("tag_name");

            if(remoteVersion.equals(VERSION)) {
                return "Local app version: " + VERSION + " is up to date!";
            }

            return "Local app version: " + VERSION + " is out of date! Remote app version: " + remoteVersion + "\nCheck https://github.com/Degubi/PDFTableExtractor for new release!";
        } catch (Exception e) {
            return "Unable to get app version from repository: https://github.com/Degubi/PDFTableExtractor";
        }
    }
}