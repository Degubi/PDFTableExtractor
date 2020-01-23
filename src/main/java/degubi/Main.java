package degubi;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;

import com.google.gson.*;
import java.awt.*;
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
    public static final String VERSION = "1.0.0";
    public static final String SETTING_PARALLEL_FILEPROCESS = "parallelFileProcess";
    public static final String SETTING_ROWS_PER_PAGE = "rowsPerPage";
    public static final String SETTING_ROW_COMPARISON_METHOD = "rowComparisonMethod";
    public static final String SETTING_COLUMNS_PER_PAGE = "columnsPerPage";
    public static final String SETTING_COLUMN_COMPARISON_METHOD = "columnComparisonMethod";
    public static final String SETTING_AUTOSIZE_COLUMNS = "autosizeColumns";
    public static final String SETTING_PAGENAMING_METHOD = "pageNamingMethod";
    
    @SuppressWarnings("boxing")
    public static void main(String[] args) {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var sourceDir = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1)).getParent().toString().replace("%20", " ");
        var settingsPath = Path.of(sourceDir + "/settings.json");
        var settingsObject = readSettings(settingsPath, gson);
        var rowsPerPage = getIntSetting(SETTING_ROWS_PER_PAGE, 1, settingsObject);
        var rowComparisonMethod = getIntSetting(SETTING_ROW_COMPARISON_METHOD, 2, settingsObject);
        var columnsPerPage = getIntSetting(SETTING_COLUMNS_PER_PAGE, 1, settingsObject);
        var columnComparisonMethod = getIntSetting(SETTING_COLUMN_COMPARISON_METHOD, 2, settingsObject);
        var parallelExtraction = getBooleanSetting(SETTING_PARALLEL_FILEPROCESS, false, settingsObject);
        var autosizeColumns = getBooleanSetting(SETTING_AUTOSIZE_COLUMNS, true, settingsObject);
        var pageNamingMethod = getIntSetting(SETTING_PAGENAMING_METHOD, 0, settingsObject);
        
        if(args.length == 0) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            var frame = new JFrame("PDF To XLSX - " + VERSION);
            var panel = new JPanel(null);
            var bigBaldFont = new Font("SansSerif", Font.BOLD, 20);
            var oneThruTen = IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new);
            
            var comparisonMethods = new String[] {"less/equal", "equal", "greater/equal"};
            var pageNamingMethods = new String[] {"counting", "pageOrdinal-tableOrdinal"};
            var rowsComboBox = newCombobox(320, 50, 50, rowsPerPage, oneThruTen);
            var rowComparisonBox = newCombobox(160, 50, 100, comparisonMethods[rowComparisonMethod], comparisonMethods);
            var columnsComboBox = newCombobox(320, 90, 50, columnsPerPage, oneThruTen);
            var columnComparisonBox = newCombobox(160, 90, 100, comparisonMethods[columnComparisonMethod], comparisonMethods);
            var pageNamingComboBox = newCombobox(140, 170, 150, pageNamingMethods[pageNamingMethod], pageNamingMethods);
            var autosizeCheckBox = newCheckbox(15, 210, "Autosize Columns After Extraction", autosizeColumns);
            var parallelCheckBox = newCheckbox(15, 290, "Enable Parallel File Processing", parallelExtraction);
            
            addSettingsSection("Page Filters", 10, panel, bigBaldFont);
            panel.add(newLabel(20, 50, "Keep pages with rows:"));
            panel.add(rowsComboBox);
            panel.add(newLabel(270, 50, "than/to"));
            panel.add(rowComparisonBox);
            panel.add(newLabel(20, 90, "Keep pages with columns:"));
            panel.add(columnsComboBox);
            panel.add(newLabel(270, 90, "than/to"));
            panel.add(columnComparisonBox);
            addSettingsSection("Page Settings", 130, panel, bigBaldFont);
            panel.add(newLabel(20, 170, "Page naming strategy: "));
            panel.add(pageNamingComboBox);
            panel.add(autosizeCheckBox);
            addSettingsSection("File Settings", 250, panel, bigBaldFont);
            panel.add(parallelCheckBox);
            
            frame.setContentPane(panel);
            frame.setBounds(0, 0, 600, 500);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);
            
            Runtime.getRuntime()
                   .addShutdownHook(new Thread(() -> saveSettings(gson, settingsPath, comparisonMethods, pageNamingMethods, parallelCheckBox, autosizeCheckBox,
                                                                  rowsComboBox, columnsComboBox, rowComparisonBox, columnComparisonBox, pageNamingComboBox)));
        }else{
            System.out.println("Version: " + VERSION + '\n');
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            
            var rowComparisonFunction = getComparisonFunction(rowComparisonMethod, rowsPerPage);
            var columnComparisonFunction = getComparisonFunction(columnComparisonMethod, columnsPerPage);
            var pageNamingFunction = getPageNamingFunction(pageNamingMethod);
            var textExtractor = new SpreadsheetExtractionAlgorithm();
            var pdfSrc = parallelExtraction ? Arrays.stream(args).parallel()
                                            : Arrays.stream(args);
            pdfSrc.map(Path::of)
                  .map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .forEach(inputFile -> {
                      System.out.println("Opening file: " + inputFile);
                      
                      if(!inputFile.endsWith(".pdf")) {
                          System.out.println(inputFile + " is not a pdf file");
                          return;
                      }
                      
                      try(var pdfInput = PDDocument.load(new File(inputFile));
                          var excelOutput = new XSSFWorkbook()){
                          var counters = new int[2]; //0: PDF Page Counter, 1: Table per PDF Page Counter
                          
                          System.out.println("Extracting data from: " + inputFile);
                          StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdfInput).extract(), ORDERED | IMMUTABLE), false)
                                       .map(textExtractor::extract)
                                       .peek(k -> ++counters[0])
                                       .flatMap(List::stream)
                                       .peek(k -> ++counters[1])
                                       .map(Table::getRows)
                                       .filter(tableData -> tableData.size() != 0 && rowComparisonFunction.test(tableData.size()) && columnComparisonFunction.test(tableData.get(0).size()))
                                       .forEach(tableData -> storeDataToPage(excelOutput, tableData, autosizeColumns, pageNamingFunction, counters));
                          
                          var numberOfSheets = excelOutput.getNumberOfSheets();
                          if(numberOfSheets > 0) {
                              var separatorIndex = inputFile.lastIndexOf('.');
                              var filePath = inputFile.substring(0, separatorIndex);
                              var outputFile = Path.of(filePath + ".xlsx");
                              System.out.println("Writing " + numberOfSheets + " pages to file: " + outputFile);
                              
                              try(var outputStream = Files.newOutputStream(outputFile, WRITE, CREATE, TRUNCATE_EXISTING)){
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
                  });
            
            System.out.print("All done, press enter");
            System.console().readLine();
        }
    }
    
    private static void storeDataToPage(XSSFWorkbook excelOutput, List<List<RectangularTextContainer>> tableData, boolean autosizeColumns,
                                        BiFunction<XSSFWorkbook, int[], String> pageNamingFunction, int[] counters) {
        
        var pageSheet = excelOutput.createSheet(pageNamingFunction.apply(excelOutput, counters));
        
        IntStream.range(0, tableData.size())
                 .forEach(rowIndex -> fillRowWithData(tableData, pageSheet, rowIndex));
        
        if(autosizeColumns) {
            IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                     .forEach(pageSheet::autoSizeColumn);
        }
        
        pageSheet.setActiveCell(new CellAddress(0, 0));
    }
    
    private static void fillRowWithData(List<List<RectangularTextContainer>> tableData, XSSFSheet pageSheet, int rowIndex) {
        var excelRow = pageSheet.createRow(rowIndex);
        
        IntStream.range(0, tableData.get(rowIndex).size())
                 .forEach(columnIndex -> {
                     var cellValue = tableData.get(rowIndex).get(columnIndex).getText();
                     excelRow.createCell(columnIndex).setCellValue(cellValue);
                 });
    }
    
    
    private static boolean getBooleanSetting(String setting, boolean defaultValue, JsonObject settingsObject) {
        if(settingsObject.has(setting)) {
            return settingsObject.get(setting).getAsBoolean();
        }
        
        settingsObject.addProperty(setting, Boolean.valueOf(defaultValue));
        return defaultValue;
    }
    
    private static int getIntSetting(String setting, int defaultValue, JsonObject settingsObject) {
        if(settingsObject.has(setting)) {
            return settingsObject.get(setting).getAsInt();
        }
        
        settingsObject.addProperty(setting, Integer.valueOf(defaultValue));
        return defaultValue;
    }
    
    private static JsonObject readSettings(Path settingsPath, Gson gson) {
        try {
            return gson.fromJson(Files.readString(settingsPath), JsonObject.class);
        } catch (IOException e) {
            return new JsonObject();
        }
    }
    
    private static void saveSettings(Gson gson, Path settingsPath, String[] comparisonMethods, String[] pageNamingMethods, JCheckBox parallelCheckbox, JCheckBox autosizeColumns,
                                     JComboBox<Integer> rowsPerPageSelector, JComboBox<Integer> columnsPerPageSelector, 
                                     JComboBox<String> rowComparisonSelector, JComboBox<String> columnComparisonSelector,
                                     JComboBox<String> pageNamingComboBox) {
        
        var settings = new JsonObject();
        settings.addProperty(SETTING_ROWS_PER_PAGE, (Integer) rowsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_ROW_COMPARISON_METHOD, Integer.valueOf(indexOf((String) rowComparisonSelector.getSelectedItem(), comparisonMethods)));
        settings.addProperty(SETTING_COLUMNS_PER_PAGE, (Integer) columnsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_COLUMN_COMPARISON_METHOD, Integer.valueOf(indexOf((String) columnComparisonSelector.getSelectedItem(), comparisonMethods)));
        settings.addProperty(SETTING_AUTOSIZE_COLUMNS, Boolean.valueOf(autosizeColumns.isSelected()));
        settings.addProperty(SETTING_PARALLEL_FILEPROCESS, Boolean.valueOf(parallelCheckbox.isSelected()));
        settings.addProperty(SETTING_PAGENAMING_METHOD, Integer.valueOf(indexOf((String) pageNamingComboBox.getSelectedItem(), pageNamingMethods)));
        
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
        checkbox.setBounds(x, y, text.length() * 6, 30);
        checkbox.setFocusPainted(false);
        return checkbox;
    }
    
    @SafeVarargs
    private static<T> JComboBox<T> newCombobox(int x, int y, int width, T settingsValue, T... elements) {
        var wombocombo = new JComboBox<>(elements);
        wombocombo.setBounds(x, y, width, 30);
        wombocombo.setFocusable(false);
        
        var elementIndex = indexOf(settingsValue, elements);
        if(elementIndex != -1) {
            wombocombo.setSelectedIndex(elementIndex);
        }
        return wombocombo;
    }
    
    private static void addSettingsSection(String text, int y, JPanel contentPanel, Font font) {
        var label = new JLabel(text);
        label.setBounds(20, y, text.length() * 12, 30);
        label.setFont(font);
        contentPanel.add(label);
        
        var separatorBottom = new JSeparator(SwingConstants.HORIZONTAL);
        separatorBottom.setBounds(0, y + 30, 600, 2);
        contentPanel.add(separatorBottom);
    }
    
    
    private static BiFunction<XSSFWorkbook, int[], String> getPageNamingFunction(int namingMethod) {
        return namingMethod == 0 ? (workbook, counters) -> (workbook.getNumberOfSheets() + 1) + "."
                                 : (workbook, counters) -> counters[0] + ". Page " + counters[1] + ". Table";
    }
    
    private static IntPredicate getComparisonFunction(int comparisonMethod, int value) {
        return comparisonMethod == 0 ? k -> k <= value :
               comparisonMethod == 1 ? k -> k == value :
                                       k -> k >= value;
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