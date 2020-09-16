package degubi;

import static degubi.Settings.*;
import static java.util.Spliterator.*;

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
import jakarta.json.*;
import jakarta.json.bind.*;
import javax.swing.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

public final class Main {
    public static final String VERSION = "1.2.0";
    public static final Jsonb json = JsonbBuilder.create(new JsonbConfig().withFormatting(Boolean.TRUE));

    @SuppressWarnings("boxing")
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            var oneThruTen = IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new);
            var comparisonMethods = new String[] {"less/equal", "equal", "greater/equal"};

            var rowsPerPageSelector = Components.newComboBox(360, 50, 50, rowsPerPage, oneThruTen);
            var rowComparisonSelector = Components.newComboBox(200, 50, 100, comparisonMethods[rowComparisonMethod], comparisonMethods);
            var columnsPerPageSelector = Components.newComboBox(360, 90, 50, columnsPerPage, oneThruTen);
            var columnComparisonSelector = Components.newComboBox(200, 90, 100, comparisonMethods[columnComparisonMethod], comparisonMethods);

            var panel = new JPanel(null);
            var bigBaldFont = new Font("SansSerif", Font.BOLD, 20);
            var emptyColumnSkipGroup = new ButtonGroup();
            var emptyRowSkipGroup = new ButtonGroup();

            Components.addSettingsSection("Page Filters", 10, panel, bigBaldFont);
            panel.add(Components.newLabel(20, 50, "Keep pages with number of rows"));
            panel.add(rowsPerPageSelector);
            panel.add(Components.newLabel(310, 50, "than/to"));
            panel.add(rowComparisonSelector);
            panel.add(Components.newLabel(20, 90, "Keep pages with number of columns"));
            panel.add(columnsPerPageSelector);
            panel.add(Components.newLabel(310, 90, "than/to"));
            panel.add(columnComparisonSelector);
            panel.add(Components.newLabel(20, 130, "Skip empty rows:"));
            panel.add(Components.newRadioButton(155, 130, 50, "None", 0, emptyRowSkipMethod == 0, emptyRowSkipGroup));
            panel.add(Components.newRadioButton(220, 130, 65, "Leading", 1, emptyRowSkipMethod == 1, emptyRowSkipGroup));
            panel.add(Components.newRadioButton(300, 130, 60, "Trailing", 2, emptyRowSkipMethod == 2, emptyRowSkipGroup));
            panel.add(Components.newRadioButton(380, 130, 55, "Both", 3, emptyRowSkipMethod == 3, emptyRowSkipGroup));
            panel.add(Components.newLabel(20, 160, "Skip empty columns:"));
            panel.add(Components.newRadioButton(155, 160, 50, "None", 0, emptyColumnSkipMethod == 0, emptyColumnSkipGroup));
            panel.add(Components.newRadioButton(220, 160, 65, "Leading", 1, emptyColumnSkipMethod == 1, emptyColumnSkipGroup));
            panel.add(Components.newRadioButton(300, 160, 60, "Trailing", 2, emptyColumnSkipMethod == 2, emptyColumnSkipGroup));
            panel.add(Components.newRadioButton(380, 160, 55, "Both", 3, emptyColumnSkipMethod == 3, emptyColumnSkipGroup));

            var pageNamingMethods = new String[] {"counting", "pageOrdinal-tableOrdinal"};
            var pageNamingComboBox = Components.newComboBox(140, 240, 150, pageNamingMethods[pageNamingMethod], pageNamingMethods);
            var autosizeColumnsCheckBox = Components.newCheckBox(15, 280, "Autosize Columns After Extraction", autosizeColumns);

            Components.addSettingsSection("Page Output", 200, panel, bigBaldFont);
            panel.add(Components.newLabel(20, 240, "Page naming strategy: "));
            panel.add(pageNamingComboBox);
            panel.add(autosizeColumnsCheckBox);

            var parallelCheckBox = Components.newCheckBox(15, 360, "Enable Parallel File Processing", parallelExtraction);
            var pdfContextMenuCheckBox = Components.newCheckBox(15, 390, "Enable PDF extraction context menu", contextMenuOptionEnabled);
            var versionCheckingDisabledBox = Components.newCheckBox(15, 420, "Disable Version Checking", versionCheckingDisabled);

            Components.addSettingsSection("App Settings", 320, panel, bigBaldFont);
            panel.add(parallelCheckBox);
            panel.add(pdfContextMenuCheckBox);
            panel.add(versionCheckingDisabledBox);

            var frame = new JFrame("PDF Table Extractor - " + VERSION);
            frame.setContentPane(panel);
            frame.setSize(600, 550);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);

            Runtime.getRuntime()
                   .addShutdownHook(new Thread(() -> {
                       var settings = Json.createObjectBuilder()
                                          .add(SETTING_ROWS_PER_PAGE, (int) rowsPerPageSelector.getSelectedItem())
                                          .add(SETTING_ROW_COMPARISON_METHOD, indexOf((String) rowComparisonSelector.getSelectedItem(), comparisonMethods))
                                          .add(SETTING_COLUMNS_PER_PAGE, (int) columnsPerPageSelector.getSelectedItem())
                                          .add(SETTING_COLUMN_COMPARISON_METHOD, indexOf((String) columnComparisonSelector.getSelectedItem(), comparisonMethods))
                                          .add(SETTING_AUTOSIZE_COLUMNS, autosizeColumnsCheckBox.isSelected())
                                          .add(SETTING_PARALLEL_FILEPROCESS, parallelCheckBox.isSelected())
                                          .add(SETTING_PAGENAMING_METHOD, indexOf((String) pageNamingComboBox.getSelectedItem(), pageNamingMethods))
                                          .add(SETTING_EMPTY_COLUMN_SKIP_METHOD, emptyColumnSkipGroup.getSelection().getMnemonic())
                                          .add(SETTING_EMPTY_ROW_SKIP_METHOD, emptyRowSkipGroup.getSelection().getMnemonic())
                                          .add(SETTING_VERSION_CHECKING_DISABLED, versionCheckingDisabledBox.isSelected())
                                          .add(SETTING_CONTEXT_MENU_OPTION_ENABLED, pdfContextMenuCheckBox.isSelected())
                                          .build();

                       saveSettings(settings, pdfContextMenuCheckBox.isSelected());
                }));
        }else{
            var versionCheckResult = CompletableFuture.supplyAsync(Main::createVersionCheckingTask);

            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

            var rowComparisonFunction = getComparisonFunction(rowComparisonMethod, rowsPerPage);
            var columnComparisonFunction = getComparisonFunction(columnComparisonMethod, columnsPerPage);
            var pageNamingFunction = getPageNamingFunction(pageNamingMethod);
            var pdfSrc = parallelExtraction ? Arrays.stream(args).parallel()
                                            : Arrays.stream(args);
            pdfSrc.map(Path::of)
                  .map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .peek(k -> System.out.println("Opening file: " + k))
                  .filter(Main::checkFileExtension)
                  .forEach(inputFile -> handlePDFExtraction(rowComparisonFunction, columnComparisonFunction, pageNamingFunction, inputFile));

            try{
                System.out.println(versionCheckResult.get(15, TimeUnit.SECONDS));
            }catch(TimeoutException e){
                System.out.println("Version checking timed out... :/");
            }

            System.out.print("All done, press enter");
            System.console().readLine();
        }
    }

    private static void handlePDFExtraction(IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction, PageNamingFunction pageNamingFunction, String inputFile) {
        try(var pdfInput = PDDocument.load(new File(inputFile));
            var excelOutput = new XSSFWorkbook()){

            System.out.println("Extracting data from: " + inputFile);
            extractPDF(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, pdfInput, excelOutput);

            var numberOfSheets = excelOutput.getNumberOfSheets();
            if(numberOfSheets > 0) {
                var filePath = inputFile.substring(0, inputFile.lastIndexOf('.'));
                var outputFile = Path.of(filePath + ".xlsx");
                System.out.println("Writing " + numberOfSheets + " pages to file: " + outputFile);

                try(var outputStream = Files.newOutputStream(outputFile)){
                    excelOutput.write(outputStream);
                }

                System.out.println("Finished: " + outputFile + '\n');
            }else{
                System.out.println("No pages were extracted from file: " + inputFile);
            }
        }catch(Exception e) {
            System.out.println("An error happened, check error.txt for details");

            try(var exceptionOutput = new PrintStream("error.txt")){
                e.printStackTrace(exceptionOutput);
            } catch (FileNotFoundException e1) {}
        }
    }

    @SuppressWarnings({"unchecked", "cast"})
    public static void extractPDF(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                  PageNamingFunction pageNamingFunction, PDDocument pdfInput, XSSFWorkbook excelOutput) {

        var textExtractor = new SpreadsheetExtractionAlgorithm();
        var rawPages = StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdfInput).extract(), ORDERED | IMMUTABLE), false)
                                    .map(textExtractor::extract)
                                    .toArray(List[]::new);

        var extractedPages = (List<Table>[]) rawPages;   //Gotta love Java's non generic arrays, this cast is fine

        IntStream.range(0, extractedPages.length)
                 .forEach(pageIndex -> {
                     var tables = extractedPages[pageIndex];

                     IntStream.range(0, tables.size())
                              .forEach(tableIndex -> writeTable(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction,
                                                                columnComparisonFunction, pageNamingFunction, excelOutput, pageIndex, tables, tableIndex));
       });
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

    private static boolean checkFileExtension(String filePath) {
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

    private static String createVersionCheckingTask() {
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

    private static<T> int indexOf(T element, T[] array) {
        for(var i = 0; i < array.length; ++i) {
            if(array[i].equals(element)) {
                return i;
            }
        }
        return -1;
    }
}