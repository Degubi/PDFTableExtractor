package degubi;

import static degubi.Main.RowProviderFunction.*;
import static degubi.Main.RowWalkerFunction.*;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Spliterator.*;
import static java.util.stream.IntStream.*;

import com.google.gson.*;
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
    public static final String VERSION = "1.0.0";
    public static final String SETTING_PARALLEL_FILEPROCESS = "parallelFileProcess";
    public static final String SETTING_ROWS_PER_PAGE = "rowsPerPage";
    public static final String SETTING_ROW_COMPARISON_METHOD = "rowComparisonMethod";
    public static final String SETTING_COLUMNS_PER_PAGE = "columnsPerPage";
    public static final String SETTING_COLUMN_COMPARISON_METHOD = "columnComparisonMethod";
    public static final String SETTING_AUTOSIZE_COLUMNS = "autosizeColumns";
    public static final String SETTING_PAGENAMING_METHOD = "pageNamingMethod";
    public static final String SETTING_EMPTY_COLUMN_SKIP_METHOD = "emptyColumnSkipMethod";
    public static final String SETTING_EMPTY_ROW_SKIP_METHOD = "emptyRowSkipMethod";
    
    @SuppressWarnings("boxing")
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var versionCheckResult = CompletableFuture.supplyAsync(() -> createVersionCheckingTask(gson));
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
        var emptyColumnSkipMethod = getIntSetting(SETTING_EMPTY_COLUMN_SKIP_METHOD, 0, settingsObject);
        var emptyRowSkipMethod = getIntSetting(SETTING_EMPTY_ROW_SKIP_METHOD, 0, settingsObject);
        
        if(args.length == 0) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            var oneThruTen = IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new);
            var comparisonMethods = new String[] {"less/equal", "equal", "greater/equal"};
            var pageNamingMethods = new String[] {"counting", "pageOrdinal-tableOrdinal"};
            
            var rowsComboBox = newComboBox(320, 50, 50, rowsPerPage, oneThruTen);
            var rowComparisonBox = newComboBox(160, 50, 100, comparisonMethods[rowComparisonMethod], comparisonMethods);
            var columnsComboBox = newComboBox(320, 90, 50, columnsPerPage, oneThruTen);
            var columnComparisonBox = newComboBox(160, 90, 100, comparisonMethods[columnComparisonMethod], comparisonMethods);
            var pageNamingComboBox = newComboBox(140, 240, 150, pageNamingMethods[pageNamingMethod], pageNamingMethods);
            var autosizeCheckBox = newCheckBox(15, 280, "Autosize Columns After Extraction", autosizeColumns);
            var parallelCheckBox = newCheckBox(15, 360, "Enable Parallel File Processing", parallelExtraction);
            
            var panel = new JPanel(null);
            var bigBaldFont = new Font("SansSerif", Font.BOLD, 20);
            var emptyColumnSkipGroup = new ButtonGroup();
            var emptyRowSkipGroup = new ButtonGroup();

            addSettingsSection("Page Filters", 10, panel, bigBaldFont);
            panel.add(newLabel(20, 50, "Keep pages with rows:"));
            panel.add(rowsComboBox);
            panel.add(newLabel(270, 50, "than/to"));
            panel.add(rowComparisonBox);
            panel.add(newLabel(20, 90, "Keep pages with columns:"));
            panel.add(columnsComboBox);
            panel.add(newLabel(270, 90, "than/to"));
            panel.add(columnComparisonBox);
            panel.add(newLabel(20, 130, "Skip empty rows:"));
            panel.add(newRadioButton(155, 130, 50, "None", 0, emptyRowSkipMethod == 0, emptyRowSkipGroup));
            panel.add(newRadioButton(220, 130, 65, "Leading", 1, emptyRowSkipMethod == 1, emptyRowSkipGroup));
            panel.add(newRadioButton(300, 130, 60, "Trailing", 2, emptyRowSkipMethod == 2, emptyRowSkipGroup));
            panel.add(newRadioButton(380, 130, 55, "Both", 3, emptyRowSkipMethod == 3, emptyRowSkipGroup));
            panel.add(newLabel(20, 160, "Skip empty columns:"));
            panel.add(newRadioButton(155, 160, 50, "None", 0, emptyColumnSkipMethod == 0, emptyColumnSkipGroup));
            panel.add(newRadioButton(220, 160, 65, "Leading", 1, emptyColumnSkipMethod == 1, emptyColumnSkipGroup));
            panel.add(newRadioButton(300, 160, 60, "Trailing", 2, emptyColumnSkipMethod == 2, emptyColumnSkipGroup));
            panel.add(newRadioButton(380, 160, 55, "Both", 3, emptyColumnSkipMethod == 3, emptyColumnSkipGroup));
            addSettingsSection("Page Settings", 200, panel, bigBaldFont);
            panel.add(newLabel(20, 240, "Page naming strategy: "));
            panel.add(pageNamingComboBox);
            panel.add(autosizeCheckBox);
            addSettingsSection("File Settings", 320, panel, bigBaldFont);
            panel.add(parallelCheckBox);
            
            var frame = new JFrame("PDF Table Extractor - " + VERSION);
            frame.setContentPane(panel);
            frame.setBounds(0, 0, 600, 500);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setVisible(true);
            
            Runtime.getRuntime()
                   .addShutdownHook(new Thread(() -> saveSettings(gson, settingsPath, comparisonMethods, pageNamingMethods, parallelCheckBox, autosizeCheckBox,
                                                                  rowsComboBox, columnsComboBox, rowComparisonBox, columnComparisonBox, pageNamingComboBox, emptyColumnSkipGroup, emptyRowSkipGroup)));
        }else{
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
                          
                          System.out.println("Extracting data from: " + inputFile);
                          extractPDF(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, pdfInput, excelOutput, textExtractor);
                          
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
            
            try {
                System.out.println(versionCheckResult.get());
            } catch (InterruptedException | ExecutionException e) {}
            
            System.out.print("All done, press enter");
            System.console().readLine();
        }
    }

    @SuppressWarnings({"unchecked", "cast"})
    public static void extractPDF(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                  PageNamingFunction pageNamingFunction, PDDocument pdfInput, XSSFWorkbook excelOutput, SpreadsheetExtractionAlgorithm textExtractor) {
        
        var rawPages = StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdfInput).extract(), ORDERED | IMMUTABLE), false)
                                    .map(textExtractor::extract)
                                    .toArray(List[]::new);
        
        var extractedPages = (List<Table>[]) rawPages;   //Gotta love Java's non generic arrays, 'rawPages' and this cast is fine
          
        range(0, extractedPages.length)
       .forEach(pageIndex -> {
           var tables = extractedPages[pageIndex];
           
           range(0, tables.size()).forEach(tableIndex -> {
               var rawRows = tables.get(tableIndex).getRows();
               
               if(rawRows.size() > 0) {
                   var rows = rawRows.stream()
                                     .map(k -> k.stream().map(RectangularTextContainer::getText).toArray(String[]::new))
                                     .toArray(String[][]::new);
                   
                   var removableColumnCountFromBegin = (emptyColumnSkipMethod & 1) == 0 ? 0 : calculateRemovableColumnCount(rows, walkForewards());
                   var removableColumnCountFromEnd = (emptyColumnSkipMethod & 2) == 0 ? 0 : calculateRemovableColumnCount(rows, walkBackwards());
                   var removableRowCountFromBegin = (emptyRowSkipMethod & 1) == 0 ? 0 : calculateRemovableRowCount(rows, providingForewards());
                   var removableRowCountFromEnd = (emptyRowSkipMethod & 2) == 0 ? 0 : calculateRemovableRowCount(rows, providingBackwards());
                   
                   if(rowComparisonFunction.test(rows.length - removableRowCountFromBegin - removableRowCountFromEnd) && 
                      columnComparisonFunction.test(rows[0].length - removableColumnCountFromBegin - removableColumnCountFromEnd)) {
                       
                       var pageSheet = excelOutput.createSheet(pageNamingFunction.apply(excelOutput, pageIndex, tableIndex));
                       range(removableRowCountFromBegin, rows.length - removableRowCountFromEnd)
                      .forEach(rowIndex -> {
                           var excelRow = pageSheet.createRow(rowIndex);
                           
                           range(removableColumnCountFromBegin, rows[rowIndex].length - removableColumnCountFromEnd)
                          .forEach(columnIndex -> excelRow.createCell(columnIndex).setCellValue(rows[rowIndex][columnIndex]));
                       });
                       
                       if(autosizeColumns) {
                           range(0, pageSheet.getRow(0).getPhysicalNumberOfCells()).forEach(pageSheet::autoSizeColumn);
                       }
                       
                       pageSheet.setActiveCell(CellAddress.A1);
                   }
               }
           });
       });
    }

    
    //This function walks rows forewards/backwards until it finds a non blank value
    //instead of walking columns downwards/upwards, avoiding jumping between arrays
    private static int calculateRemovableColumnCount(String[][] data, RowWalkerFunction walkerFunction) {
        return Arrays.stream(data)
                     .mapToInt(columnData -> IntStream.range(0, columnData.length)
                                                      .mapToObj(columnIndex -> walkerFunction.apply(columnData, columnIndex))
                                                      .takeWhile(String::isBlank)
                                                      .mapToInt(i -> 1)
                                                      .sum())
                     .min()
                     .orElse(0);
    }
    
    //This function walks rows forewards/backwards until it finds a row that is not fully empty
    private static int calculateRemovableRowCount(String[][] data, RowProviderFunction elementProvider) {
        return IntStream.range(0, data.length)
                        .takeWhile(k -> Arrays.stream(elementProvider.apply(data, k)).allMatch(String::isBlank))
                        .map(k -> 1)
                        .sum();
    }
    
    
    private static boolean getBooleanSetting(String setting, boolean defaultValue, JsonObject settingsObject) {
        var value = settingsObject.get(setting);
        if(value != null) {
            return value.getAsBoolean();
        }
        
        settingsObject.addProperty(setting, Boolean.valueOf(defaultValue));
        return defaultValue;
    }
    
    private static int getIntSetting(String setting, int defaultValue, JsonObject settingsObject) {
        var value = settingsObject.get(setting);
        if(value != null) {
            return value.getAsInt();
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
                                     JComboBox<Integer> rowsPerPageSelector, JComboBox<Integer> columnsPerPageSelector, JComboBox<String> rowComparisonSelector,
                                     JComboBox<String> columnComparisonSelector, JComboBox<String> pageNamingComboBox, ButtonGroup emptyColumnSkipButtons, ButtonGroup emptyRowSkipButtons) {
        
        var settings = new JsonObject();
        settings.addProperty(SETTING_ROWS_PER_PAGE, (Integer) rowsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_ROW_COMPARISON_METHOD, Integer.valueOf(indexOf((String) rowComparisonSelector.getSelectedItem(), comparisonMethods)));
        settings.addProperty(SETTING_COLUMNS_PER_PAGE, (Integer) columnsPerPageSelector.getSelectedItem());
        settings.addProperty(SETTING_COLUMN_COMPARISON_METHOD, Integer.valueOf(indexOf((String) columnComparisonSelector.getSelectedItem(), comparisonMethods)));
        settings.addProperty(SETTING_AUTOSIZE_COLUMNS, Boolean.valueOf(autosizeColumns.isSelected()));
        settings.addProperty(SETTING_PARALLEL_FILEPROCESS, Boolean.valueOf(parallelCheckbox.isSelected()));
        settings.addProperty(SETTING_PAGENAMING_METHOD, Integer.valueOf(indexOf((String) pageNamingComboBox.getSelectedItem(), pageNamingMethods)));
        settings.addProperty(SETTING_EMPTY_COLUMN_SKIP_METHOD, Integer.valueOf(emptyColumnSkipButtons.getSelection().getMnemonic()));
        settings.addProperty(SETTING_EMPTY_ROW_SKIP_METHOD, Integer.valueOf(emptyRowSkipButtons.getSelection().getMnemonic()));

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
    
    private static JCheckBox newCheckBox(int x, int y, String text, boolean selected) {
        var check = new JCheckBox(text, selected);
        check.setBounds(x, y, text.length() * 6, 30);
        check.setFocusPainted(false);
        return check;
    }
    
    private static JRadioButton newRadioButton(int x, int y, int width, String text, int index, boolean selected, ButtonGroup group) {
        var butt = new JRadioButton(text, selected);
        butt.setBounds(x, y, width, 30);
        butt.setMnemonic(index);
        butt.setFocusPainted(false);
        group.add(butt);
        return butt;
    }
    
    @SafeVarargs
    private static<T> JComboBox<T> newComboBox(int x, int y, int width, T settingsValue, T... elements) {
        var wombocombo = new JComboBox<>(elements);
        wombocombo.setBounds(x, y, width, 30);
        wombocombo.setFocusable(false);
        wombocombo.setSelectedItem(settingsValue);
        return wombocombo;
    }
    
    private static void addSettingsSection(String text, int y, JPanel contentPanel, Font font) {
        var label = new JLabel(text);
        label.setBounds(20, y, text.length() * 12, 30);
        label.setFont(font);
        contentPanel.add(label);
        
        var separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setBounds(0, y + 30, 600, 2);
        contentPanel.add(separator);
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
    
    private static String createVersionCheckingTask(Gson gson) {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("https://api.github.com/repos/Degubi/PDFTableExtractor/releases/latest"))
                                 .build();
        try {
            var rawResponse = client.send(request, BodyHandlers.ofString()).body();
            var parsedResponse = gson.fromJson(rawResponse, JsonObject.class);
            var remoteVersion = parsedResponse.get("tag_name").getAsString();
            
            if(remoteVersion.equals(VERSION)) {
                return "Local app version: " + VERSION + " is up to date!";
            }
            
            return "Local app version: " + VERSION + " is out of date! Remote app version: " + remoteVersion + "Check https://github.com/Degubi/PDFTableExtractor for new release!";
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
    
    @FunctionalInterface
    public interface RowWalkerFunction {
        String apply(String[] data, int columnIndex);
        
        static RowWalkerFunction walkForewards() {
            return (row, columnIndex) -> row[columnIndex];
        }
        
        static RowWalkerFunction walkBackwards() {
            return (row, columnIndex) -> row[row.length - 1 - columnIndex];
        }
    }
    
    @FunctionalInterface
    public interface RowProviderFunction {
        String[] apply(String[][] data, int columnIndex);
        
        static RowProviderFunction providingForewards() {
            return (data, columnIndex) -> data[columnIndex];
        }
        
        static RowProviderFunction providingBackwards() {
            return (data, columnIndex) -> data[data.length - columnIndex - 1];
        }
    }
    
    @FunctionalInterface
    public interface PageNamingFunction {
        String apply(XSSFWorkbook workbook, int pageIndex, int tableIndex);
    }
}