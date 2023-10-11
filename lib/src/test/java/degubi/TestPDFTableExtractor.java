package degubi;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.*;

import pdftableextractorlib.*;

public final class TestPDFTableExtractor {

    public final PageNamingFunction countingNamingFunction = PDFTableExtractor.getPageNamingFunction(0);

    public final Path file1 = Path.of("./src/test/resources/test1.pdf");
    public final Path file2 = Path.of("./src/test/resources/test2.pdf");
    public final Path file3 = Path.of("./src/test/resources/test3.pdf");

    @Test
    public void test1NormalExtraction() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 0, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file1)){
            assertEquals(3, excelOutput.getNumberOfSheets());

            var firstSheet = excelOutput.getSheetAt(0);
            assertEquals(5, firstSheet.getPhysicalNumberOfRows());
            assertEquals(2, firstSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(firstSheet.getSheetName(), "1.");

            var secondSheet = excelOutput.getSheetAt(1);
            assertEquals(13, secondSheet.getPhysicalNumberOfRows());
            assertEquals(5, secondSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(secondSheet.getSheetName(), "2.");

            var thirdSheet = excelOutput.getSheetAt(2);
            assertEquals(6, thirdSheet.getPhysicalNumberOfRows());
            assertEquals(2, thirdSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(thirdSheet.getSheetName(), "3.");
        }
    }

    @Test
    public void test1RemoveAllEmptyNoChange() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 3, 3, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file1)){
            assertEquals(3, excelOutput.getNumberOfSheets());

            var firstSheet = excelOutput.getSheetAt(0);
            assertEquals(5, firstSheet.getPhysicalNumberOfRows());
            assertEquals(2, firstSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(firstSheet.getSheetName(), "1.");

            var secondSheet = excelOutput.getSheetAt(1);
            assertEquals(13, secondSheet.getPhysicalNumberOfRows());
            assertEquals(5, secondSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(secondSheet.getSheetName(), "2.");

            var thirdSheet = excelOutput.getSheetAt(2);
            assertEquals(6, thirdSheet.getPhysicalNumberOfRows());
            assertEquals(2, thirdSheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(thirdSheet.getSheetName(), "3.");
        }
    }

    @Test
    public void test2NormalExtraction() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 0, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file2)){
            assertEquals(2, excelOutput.getNumberOfSheets());

            IntStream.rangeClosed(0, 1)
                     .mapToObj(excelOutput::getSheetAt)
                     .forEach(sheet -> {
                         assertEquals(47, sheet.getPhysicalNumberOfRows());
                         assertEquals(4, sheet.getRow(0).getPhysicalNumberOfCells());
                     });
        }
    }

    @Test
    public void test2RemoveAllEmptyNoChange() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 3, 3, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file2)){
            assertEquals(2, excelOutput.getNumberOfSheets());

            IntStream.rangeClosed(0, 1)
                     .mapToObj(excelOutput::getSheetAt)
                     .forEach(sheet -> {
                         assertEquals(47, sheet.getPhysicalNumberOfRows());
                         assertEquals(4, sheet.getRow(0).getPhysicalNumberOfCells());
                     });
        }
    }

    @Test
    public void test3NormalExtraction() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 0, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file3)){
            assertEquals(5, excelOutput.getNumberOfSheets());

            IntStream.rangeClosed(0, 2)
                     .mapToObj(excelOutput::getSheetAt)
                     .forEach(sheet -> {
                         assertEquals(1, sheet.getPhysicalNumberOfRows());
                         assertEquals(1, sheet.getRow(0).getPhysicalNumberOfCells());
                     });

            var fourthSheet = excelOutput.getSheetAt(3);
            assertEquals(1, fourthSheet.getPhysicalNumberOfRows());
            assertEquals(6, fourthSheet.getRow(0).getPhysicalNumberOfCells());

            var fifthSheet = excelOutput.getSheetAt(4);
            assertEquals(23, fifthSheet.getPhysicalNumberOfRows());
            assertEquals(6, fifthSheet.getRow(0).getPhysicalNumberOfCells());
        }
    }

    @Test
    public void test3ExtractionWithEmptyTrailingColumns() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 2, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file3)){
            assertEquals(5, excelOutput.getNumberOfSheets());

            IntStream.rangeClosed(0, 2)
                     .mapToObj(excelOutput::getSheetAt)
                     .forEach(sheet -> {
                         assertEquals(1, sheet.getPhysicalNumberOfRows());
                         assertEquals(1, sheet.getRow(0).getPhysicalNumberOfCells());
                     });

            var fourthSheet = excelOutput.getSheetAt(3);
            assertEquals(1, fourthSheet.getPhysicalNumberOfRows());
            assertEquals(3, fourthSheet.getRow(0).getPhysicalNumberOfCells());

            var fifthSheet = excelOutput.getSheetAt(4);
            assertEquals(23, fifthSheet.getPhysicalNumberOfRows());
            assertEquals(3, fifthSheet.getRow(0).getPhysicalNumberOfCells());
        }
    }

    @Test
    public void test3ExtractionWithEmptyTrailingRows() throws Exception {
        var keepAllpagesComparisonFunction = PDFTableExtractor.getComparisonFunction(2, 1);

        try(var excelOutput = PDFTableExtractor.extract(false, 0, 2, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, file3)){
            assertEquals(5, excelOutput.getNumberOfSheets());

            IntStream.rangeClosed(0, 2)
                     .mapToObj(excelOutput::getSheetAt)
                     .forEach(sheet -> {
                         assertEquals(1, sheet.getPhysicalNumberOfRows());
                         assertEquals(1, sheet.getRow(0).getPhysicalNumberOfCells());
                     });

            var fourthSheet = excelOutput.getSheetAt(3);
            assertEquals(1, fourthSheet.getPhysicalNumberOfRows());
            assertEquals(6, fourthSheet.getRow(0).getPhysicalNumberOfCells());

            var fifthSheet = excelOutput.getSheetAt(4);
            assertEquals(1, fifthSheet.getPhysicalNumberOfRows());
            assertEquals(6, fifthSheet.getRow(0).getPhysicalNumberOfCells());
        }
    }
}