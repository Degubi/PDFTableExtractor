package degubi;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

public final class Main {
    
    public static void main(String[] args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        if(args.length == 0) {
            System.out.println("Nincs fájl kiválasztva");
            return;
        }
        
        Arrays.stream(args)
              .map(Path::of)
              .map(Path::toAbsolutePath)
              .map(Path::toString)
              .forEach(inputFile -> {
                  var separatorIndex = inputFile.lastIndexOf('.');
                  var extension = inputFile.substring(separatorIndex + 1);
                  
                  if(!extension.equals("pdf")) {
                      System.out.println("Nem pdf fájl van kiválasztva");
                      return;
                  }
                  
                  System.out.println("Reading file: " + inputFile);
                  var filePath = inputFile.substring(0, separatorIndex);
                  var outputFile = Path.of(filePath + ".xlsx");
                  try(var pdf = PDDocument.load(new File(filePath + ".pdf"));
                      var excelOutput = new XSSFWorkbook();
                      var outputStream = Files.newOutputStream(outputFile, WRITE, CREATE, TRUNCATE_EXISTING)){
                      var textExtractor = new SpreadsheetExtractionAlgorithm();
                      
                      System.out.println("Extracting data from: " + inputFile);
                      StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdf).extract(), ORDERED | IMMUTABLE), false)
                                   .map(textExtractor::extract)
                                   .flatMap(List::stream)
                                   .map(Table::getRows)
                                   .map(Main::convertToTableData)
                                   .filter(tableData -> tableData.length > 1 && tableData[0].length > 1)
                                   .forEach(tableData -> storeTableData(excelOutput, tableData));
                      
                      System.out.println("Writing excel to: " + outputFile);
                      excelOutput.write(outputStream);
                      System.out.println("Finished: " + outputFile + '\n');
                  }catch(Exception e) {
                      try(var exceptionOutput = new PrintStream("error.txt")){
                          e.printStackTrace(exceptionOutput);
                      } catch (FileNotFoundException e1) {}
                  }
              });
        
        System.out.print("All done");
    }

    private static void storeTableData(XSSFWorkbook excelOutput, String[][] tableData) {
        var pageSheet = excelOutput.createSheet((excelOutput.getNumberOfSheets() + 1) + ".");
         
         IntStream.range(0, tableData.length)
                  .forEach(rowIndex -> fillWithData(tableData, pageSheet, rowIndex));
         
         IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                  .forEach(pageSheet::autoSizeColumn);
         
         pageSheet.setActiveCell(new CellAddress(0, 0));
    }
    
    private static String[][] convertToTableData(@SuppressWarnings("rawtypes") List<List<RectangularTextContainer>> rows){
        return rows.stream()
                   .map(row -> row.stream().map(RectangularTextContainer::getText).toArray(String[]::new))
                   .toArray(String[][]::new);
    }

    private static void fillWithData(String[][] textData, XSSFSheet pageSheet, int rowIndex) {
        var excelRow = pageSheet.createRow(rowIndex);
        
        IntStream.range(0, textData[rowIndex].length)
                 .forEach(columnIndex -> excelRow.createCell(columnIndex).setCellValue(textData[rowIndex][columnIndex]));
    }
}