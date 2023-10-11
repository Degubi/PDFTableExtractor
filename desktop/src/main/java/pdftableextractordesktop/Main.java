package pdftableextractordesktop;

import static pdftableextractordesktop.Settings.*;

import jakarta.json.*;
import jakarta.json.bind.*;
import pdftableextractorlib.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javax.swing.*;

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

            var rowComparisonFunction = PDFTableExtractor.getComparisonFunction(rowComparisonMethod, rowsPerPage);
            var columnComparisonFunction = PDFTableExtractor.getComparisonFunction(columnComparisonMethod, columnsPerPage);
            var pageNamingFunction = PDFTableExtractor.getPageNamingFunction(pageNamingMethod);
            var outputPathCreatorFunction = getOutputPathCreatorFunction(outputDirectoryMehod);
            var pdfSrc = parallelExtraction ? Arrays.stream(args).parallel()
                                            : Arrays.stream(args);
            pdfSrc.map(Path::of)
                  .map(Path::toAbsolutePath)
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

    private static void handlePDFExtraction(Path inputFile, PageNamingFunction pageNamingFunction, Function<Path, String> outputPathCreatorFunction,
                                            IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction) {

        System.out.println("Extracting data from: " + inputFile);

        try(var excelOutput = PDFTableExtractor.extract(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, inputFile)) {
            var numberOfSheets = excelOutput.getNumberOfSheets();
            if(numberOfSheets > 0) {
                var outputFile = Path.of(outputPathCreatorFunction.apply(inputFile) + ".xlsx");

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

    private static boolean checkIsPdfFile(Path filePath) {
        if(!filePath.toString().endsWith(".pdf")) {
            System.out.println(filePath + " is not a pdf file");
            return false;
        }

        return true;
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