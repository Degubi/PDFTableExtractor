package degubi;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;
import javax.swing.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

@SuppressWarnings("rawtypes")
public final class Main {
    public static final String SETTING_PARALLEL_EXTRACTION = "parallelExtraction";
    public static final String SETTING_PAGEFILTERS = "pageFilters";
    public static final String FILTER_EMPTYPAGE = "emptyPage";
    public static final String FILTER_SINGLECELL = "singleCellPage";
    
    public static void main(String[] args) {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var sourceDir = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1)).getParent().toString().replace("%20", " ");
        var settingsPath = Path.of(sourceDir + "/settings.json");
        var settings = readSettings(settingsPath, gson);
        var filters = getArraySetting(SETTING_PAGEFILTERS, settings);
        var parallelExtraction = getBooleanSetting(SETTING_PARALLEL_EXTRACTION, false, settings);
        
        if(args.length == 0) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            var filtersList = filters.collect(Collectors.toList());
            var frame = new JFrame("Settings");
            var panel = new JPanel(null);
            var parallelCheckBox = newCheckbox(100, 60, "Parallel PDF Extraction", parallelExtraction);
            var filterCheckboxes = new JCheckBox[] {newCheckbox(100, 30, "Empty Page Filter", filtersList.contains(FILTER_EMPTYPAGE)),
                                                    newCheckbox(220, 30, "Single Cell Page Filter", filtersList.contains(FILTER_SINGLECELL))};
            
            panel.add(newLabel(20, 30, "Filters:"));
            Arrays.stream(filterCheckboxes).forEach(panel::add);
            panel.add(newLabel(20, 60, "Other:"));
            panel.add(parallelCheckBox);
            
            frame.setContentPane(panel);
            frame.setBounds(0, 0, 600, 400);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> saveSettings(gson, settingsPath, settings, filterCheckboxes, parallelCheckBox)));
        }else{
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            
            var pageFilterFunction = filters.map(Main::getPageFilterFunction)
                                            .reduce(k -> true, Predicate::and);
            
            var pdfSrc = parallelExtraction ? Arrays.stream(args).parallel()
                                            : Arrays.stream(args);
            pdfSrc.map(Path::of)
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
    }

    private static Predicate<List<List<RectangularTextContainer>>> getPageFilterFunction(String filterName){
        switch(filterName) {  //TODO: Make this a return expression switch in Java14
            case FILTER_EMPTYPAGE : return tableData -> tableData.size() > 0;
            case FILTER_SINGLECELL : return tableData -> tableData.size() > 1 && tableData.get(0).size() > 1;
            default : {
                System.out.println("\nUnknown filter mode: " + filterName + ", skipping");
                return k -> true;
            }
        }
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
    
    
    private static boolean getBooleanSetting(String setting, boolean defaultValue, JsonObject settings) {
        if(settings.has(setting)) {
            return settings.get(setting).getAsBoolean();
        }
        
        settings.addProperty(setting, Boolean.valueOf(defaultValue));
        return defaultValue;
    }
    
    private static Stream<String> getArraySetting(String setting, JsonObject settings) {
        var values = settings.get(setting);
        
        if(values == null) {
            var emptyArray = new JsonArray();
            settings.add(setting, emptyArray);
            values = emptyArray;
        }
        
        return StreamSupport.stream(values.getAsJsonArray().spliterator(), false)
                            .map(JsonElement::getAsString);
    }
    
    private static JsonObject readSettings(Path settingsPath, Gson gson) {
        try {
            var settingsStr = Files.readString(settingsPath);
            
            return gson.fromJson(settingsStr, JsonObject.class);
        } catch (IOException e) {
            var defaultSettings = new JsonObject();
            defaultSettings.add(SETTING_PAGEFILTERS, new JsonArray());
            
            writeStringToFile(settingsPath, gson.toJson(defaultSettings));
            return defaultSettings;
        }
    }
    
    private static void saveSettings(Gson gson, Path settingsPath, JsonObject settings, JCheckBox[] filterCheckboxes, JCheckBox parallelCheckbox) {
        var newFiltersArray = new JsonArray();
        if(filterCheckboxes[0].isSelected()) newFiltersArray.add(FILTER_EMPTYPAGE);
        if(filterCheckboxes[1].isSelected()) newFiltersArray.add(FILTER_SINGLECELL);
        
        settings.add(SETTING_PAGEFILTERS, newFiltersArray);
        settings.addProperty(SETTING_PARALLEL_EXTRACTION, Boolean.valueOf(parallelCheckbox.isSelected()));
        
        writeStringToFile(settingsPath, gson.toJson(settings));
    }
    
    
    private static JLabel newLabel(int x, int y, String text) {
        var label = new JLabel(text);
        label.setBounds(x, y, text.length() * 7, 30);
        return label;
    }
    
    private static JCheckBox newCheckbox(int x, int y, String text, boolean selected) {
        var checkbox = new JCheckBox(text, selected);
        checkbox.setBounds(x, y, text.length() * 7, 30);
        checkbox.setFocusPainted(false);
        return checkbox;
    }
    
    
    private static void writeStringToFile(Path filePath, String content) {
        try {
            Files.writeString(filePath, content, WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("Unable to write to file: " + filePath);
        }
    }
}