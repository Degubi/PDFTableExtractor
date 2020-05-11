package degubi;

import static java.nio.file.StandardOpenOption.*;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public final class Settings {
    public static final String SETTING_PARALLEL_FILEPROCESS = "parallelFileProcess";
    public static final String SETTING_ROWS_PER_PAGE = "rowsPerPage";
    public static final String SETTING_ROW_COMPARISON_METHOD = "rowComparisonMethod";
    public static final String SETTING_COLUMNS_PER_PAGE = "columnsPerPage";
    public static final String SETTING_COLUMN_COMPARISON_METHOD = "columnComparisonMethod";
    public static final String SETTING_AUTOSIZE_COLUMNS = "autosizeColumns";
    public static final String SETTING_PAGENAMING_METHOD = "pageNamingMethod";
    public static final String SETTING_EMPTY_COLUMN_SKIP_METHOD = "emptyColumnSkipMethod";
    public static final String SETTING_EMPTY_ROW_SKIP_METHOD = "emptyRowSkipMethod";
    public static final String SETTING_VERSION_CHECKING_DISABLED = "versionCheckingDisabled";
    
    public static final boolean versionCheckingDisabled;
    public static final int rowsPerPage;
    public static final int rowComparisonMethod;
    public static final int columnsPerPage;
    public static final int columnComparisonMethod;
    public static final boolean parallelExtraction;
    public static final boolean autosizeColumns;
    public static final int pageNamingMethod;
    public static final int emptyColumnSkipMethod;
    public static final int emptyRowSkipMethod;
    
    static {
        var settingsObject = readSettings(Main.gson);

        versionCheckingDisabled = getBooleanSetting(SETTING_VERSION_CHECKING_DISABLED, false, settingsObject);
        rowsPerPage = getIntSetting(SETTING_ROWS_PER_PAGE, 1, settingsObject);
        rowComparisonMethod = getIntSetting(SETTING_ROW_COMPARISON_METHOD, 2, settingsObject);
        columnsPerPage = getIntSetting(SETTING_COLUMNS_PER_PAGE, 1, settingsObject);
        columnComparisonMethod = getIntSetting(SETTING_COLUMN_COMPARISON_METHOD, 2, settingsObject);
        parallelExtraction = getBooleanSetting(SETTING_PARALLEL_FILEPROCESS, false, settingsObject);
        autosizeColumns = getBooleanSetting(SETTING_AUTOSIZE_COLUMNS, true, settingsObject);
        pageNamingMethod = getIntSetting(SETTING_PAGENAMING_METHOD, 0, settingsObject);
        emptyColumnSkipMethod = getIntSetting(SETTING_EMPTY_COLUMN_SKIP_METHOD, 0, settingsObject);
        emptyRowSkipMethod = getIntSetting(SETTING_EMPTY_ROW_SKIP_METHOD, 0, settingsObject);
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
    
    private static JsonObject readSettings(Gson gson) {
        try {
            return gson.fromJson(Files.readString(Path.of("settings.json")), JsonObject.class);
        } catch (IOException e) {
            System.out.println("No settings file, using default settings!");
            return new JsonObject();
        }
    }
    
    public static void saveSettings(JsonObject settings) {
        try {
            Files.writeString(Path.of("settings.json"), Main.gson.toJson(settings), WRITE, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Settings() {}
}