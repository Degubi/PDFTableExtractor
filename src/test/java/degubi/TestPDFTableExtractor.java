package degubi;

import static org.junit.jupiter.api.Assertions.*;

import degubi.Main.*;
import java.io.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.junit.jupiter.api.*;
import technology.tabula.extractors.*;

public final class TestPDFTableExtractor {
    
    public static final SpreadsheetExtractionAlgorithm textExtractor = new SpreadsheetExtractionAlgorithm();
    public static final PageNamingFunction countingNamingFunction = Main.getPageNamingFunction(0);
    
    @Test
    public void testNormalExtraction1() throws Exception {
        var keepAllpagesComparisonFunction = Main.getComparisonFunction(2, 1);
        
        try(var pdfInput = PDDocument.load(new File("./src/test/resources/test1.pdf"));
            var excelOutput = new XSSFWorkbook()){
            
            Main.extractPDF(false, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, pdfInput, excelOutput, textExtractor);
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
    public void testNormalExtraction2() throws Exception {
        var keepAllpagesComparisonFunction = Main.getComparisonFunction(2, 1);
        
        try(var pdfInput = PDDocument.load(new File("./src/test/resources/test2.pdf"));
            var excelOutput = new XSSFWorkbook()){
            
            Main.extractPDF(false, 0, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, pdfInput, excelOutput, textExtractor);
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
    public void testExtractionWithEmptyTrailingColumns() throws Exception {
        var keepAllpagesComparisonFunction = Main.getComparisonFunction(2, 1);
        
        try(var pdfInput = PDDocument.load(new File("./src/test/resources/test3.pdf"));
            var excelOutput = new XSSFWorkbook()){
            
            Main.extractPDF(false, 2, keepAllpagesComparisonFunction, keepAllpagesComparisonFunction, countingNamingFunction, pdfInput, excelOutput, textExtractor);
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
}