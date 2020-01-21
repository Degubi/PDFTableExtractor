package degubi;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import javax.swing.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

@SuppressWarnings("rawtypes")
public final class Main {
    public static final String VERSION = "1.0.0";
    public static final String SETTING_PARALLEL_EXTRACTION = "parallelExtraction";
    public static final String SETTING_MIN_ROWS_PER_PAGE = "minRowsPerPage";
    public static final String SETTING_MIN_COLUMNS_PER_PAGE = "minColumnsPerPage";
    public static final String SETTING_AUTOSIZE_COLUMNS = "autosizeColumns";
    
    public static void main(String[] args) {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var sourceDir = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1)).getParent().toString().replace("%20", " ");
        var settingsPath = Path.of(sourceDir + "/settings.json");
        var settings = readSettings(settingsPath, gson);
        var minRowsPerPage = getIntSetting(SETTING_MIN_ROWS_PER_PAGE, 1, settings);
        var minColumnsPerPage = getIntSetting(SETTING_MIN_COLUMNS_PER_PAGE, 1, settings);
        var parallelExtraction = getBooleanSetting(SETTING_PARALLEL_EXTRACTION, false, settings);
        var autosizeColumns = getBooleanSetting(SETTING_AUTOSIZE_COLUMNS, true, settings);
        
        if(args.length == 0) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            var frame = new JFrame("PDF To XLSX - " + VERSION);
            var panel = new JPanel(null);
            var oneThruTen = IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new);
            var minRowsComboBox = newCombobox(200, 30, minRowsPerPage, oneThruTen);
            var minColumnsComboBox = newCombobox(200, 70, minColumnsPerPage, oneThruTen);
            var parallelCheckBox = newCheckbox(100, 110, "Parallel Extraction", parallelExtraction);
            var autosizeCheckBox = newCheckbox(260, 110, "Autosize Columns After Extraction", autosizeColumns);
            
            panel.add(newLabel(20, 30, "Min. number of rows per page:"));
            panel.add(minRowsComboBox);
            panel.add(newLabel(20, 70, "Min. number of columns per page:"));
            panel.add(minColumnsComboBox);
            panel.add(newLabel(20, 110, "Other:"));
            panel.add(parallelCheckBox);
            panel.add(autosizeCheckBox);
            
            frame.setContentPane(panel);
            frame.setBounds(0, 0, 600, 400);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> saveSettings(gson, settingsPath, settings, parallelCheckBox, autosizeCheckBox, minRowsComboBox, minColumnsComboBox)));
        }else{
            System.out.println("Version: " + VERSION + '\n');
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            
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
                                       .filter(tableData -> tableData.size() >= minRowsPerPage && tableData.get(0).size() >= minColumnsPerPage)
                                       .forEach(tableData -> storeTableData(excelOutput, tableData, autosizeColumns));
                          
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
    
    private static void storeTableData(XSSFWorkbook excelOutput, List<List<RectangularTextContainer>> tableData, boolean autosizeColumns) {
        var pageSheet = excelOutput.createSheet((excelOutput.getNumberOfSheets() + 1) + ".");
         
         IntStream.range(0, tableData.size())
                  .forEach(rowIndex -> fillWithData(tableData, pageSheet, rowIndex));
         
         if(autosizeColumns) {
             IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                      .forEach(pageSheet::autoSizeColumn);
         }
             
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
    
    private static int getIntSetting(String setting, int defaultValue, JsonObject settings) {
        if(settings.has(setting)) {
            return settings.get(setting).getAsInt();
        }
        
        settings.addProperty(setting, Integer.valueOf(defaultValue));
        return defaultValue;
    }
    
    private static JsonObject readSettings(Path settingsPath, Gson gson) {
        try {
            var settingsStr = Files.readString(settingsPath);
            
            return gson.fromJson(settingsStr, JsonObject.class);
        } catch (IOException e) {
            return new JsonObject();
        }
    }
    
    private static void saveSettings(Gson gson, Path settingsPath, JsonObject settings, JCheckBox parallelCheckbox, JCheckBox autosizeColumns,
                                     JComboBox<Integer> minRowsPerPageSelector, JComboBox<Integer> minColumnsPerPageSelector) {
        
        settings.addProperty(SETTING_MIN_ROWS_PER_PAGE, (Integer) minRowsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_MIN_COLUMNS_PER_PAGE, (Integer) minColumnsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_AUTOSIZE_COLUMNS, Boolean.valueOf(autosizeColumns.isSelected()));
        settings.addProperty(SETTING_PARALLEL_EXTRACTION, Boolean.valueOf(parallelCheckbox.isSelected()));
        
        try {
            Files.writeString(settingsPath, gson.toJson(settings), WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("Unable to write the settings file!");
        }
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
    
    @SuppressWarnings("boxing")
    @SafeVarargs
    private static<T> JComboBox<T> newCombobox(int x, int y, int settingsValue, T... elements) {
        var wombocombo = new JComboBox<>(elements);
        wombocombo.setBounds(x, y, 100, 30);
        
        for(var i = 0; i < elements.length; ++i) {
            if(elements[i].equals(settingsValue)) {
                wombocombo.setSelectedIndex(i);
                break;
            }
        }
        return wombocombo;
    }
}