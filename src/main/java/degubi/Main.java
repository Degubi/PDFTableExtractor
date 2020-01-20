package degubi;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

@SuppressWarnings("rawtypes")
public final class Main {
    
    public static void main(String[] args) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        System.out.print("Filters: ");
        var argsSeparated = Arrays.stream(args).collect(Collectors.partitioningBy(k -> k.charAt(0) == '-'));
        var pageFilterFunction = argsSeparated.get(Boolean.TRUE).stream()
                                              .peek(k -> System.out.print(k + " "))
                                              .map(Main::getPageFilterFunction)
                                              .reduce(k -> true, Predicate::and);
        System.out.println('\n');
        
        var inputFiles = argsSeparated.get(Boolean.FALSE);
        if(inputFiles.size() == 0) {
            System.out.println("No files were defined for extraction");
            return;
        }
        
        inputFiles.stream()
                  .map(Path::of)
                  .map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .forEach(inputFile -> {
                      var separatorIndex = inputFile.lastIndexOf('.');
                      var extension = inputFile.substring(separatorIndex + 1);
                      
                      if(!extension.equals("pdf")) {
                          System.out.println(inputFile + " is not a pdf file");
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
                                       .filter(pageFilterFunction)
                                       .forEach(tableData -> storeTableData(excelOutput, tableData));
                          
                          System.out.println("Writing excel to: " + outputFile);
                          excelOutput.write(outputStream);
                          System.out.println("Finished: " + outputFile + '\n');
                      }catch(Exception e) {
                          System.out.println("An error happened, check error.txt for details");
                          
                          try(var exceptionOutput = new PrintStream("error.txt")){
                              e.printStackTrace(exceptionOutput);
                          } catch (FileNotFoundException e1) {}
                      }
                  });
        
        System.out.print("All done, press enter");
        System.console().readLine();
    }

    private static Predicate<List<List<RectangularTextContainer>>> getPageFilterFunction(String mode){
        if(mode.equals("-SingleCell")) {
            return tableData -> tableData.size() > 1 && tableData.get(0).size() > 1;
        }
        
        System.out.println("\nUnknown filter mode: " + mode + ", skipping");
        return k -> true;
    }
    
    private static void storeTableData(XSSFWorkbook excelOutput, List<List<RectangularTextContainer>> tableData) {
        var pageSheet = excelOutput.createSheet((excelOutput.getNumberOfSheets() + 1) + ".");
         
         IntStream.range(0, tableData.size())
                  .forEach(rowIndex -> fillWithData(tableData, pageSheet, rowIndex));
         
         IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                  .forEach(pageSheet::autoSizeColumn);
         
         pageSheet.setActiveCell(new CellAddress(0, 0));
    }
    
    private static void fillWithData(List<List<RectangularTextContainer>> tableData, XSSFSheet pageSheet, int rowIndex) {
        var excelRow = pageSheet.createRow(rowIndex);
        
        IntStream.range(0, tableData.get(rowIndex).size())
                 .forEach(columnIndex -> {
                     var cellValue = tableData.get(rowIndex).get(columnIndex).getText();
                     excelRow.createCell(columnIndex).setCellValue(cellValue);
                 });
    }
}