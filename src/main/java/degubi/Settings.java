package degubi;

import java.io.*;
import java.nio.file.*;
import javax.json.*;

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
        var settings = readSettings();

        versionCheckingDisabled = settings.getBoolean(SETTING_VERSION_CHECKING_DISABLED, false);
        rowsPerPage = settings.getInt(SETTING_ROWS_PER_PAGE, 1);
        rowComparisonMethod = settings.getInt(SETTING_ROW_COMPARISON_METHOD, 2);
        columnsPerPage = settings.getInt(SETTING_COLUMNS_PER_PAGE, 1);
        columnComparisonMethod = settings.getInt(SETTING_COLUMN_COMPARISON_METHOD, 2);
        parallelExtraction = settings.getBoolean(SETTING_PARALLEL_FILEPROCESS, false);
        autosizeColumns = settings.getBoolean(SETTING_AUTOSIZE_COLUMNS, true);
        pageNamingMethod = settings.getInt(SETTING_PAGENAMING_METHOD, 0);
        emptyColumnSkipMethod = settings.getInt(SETTING_EMPTY_COLUMN_SKIP_METHOD, 0);
        emptyRowSkipMethod = settings.getInt(SETTING_EMPTY_ROW_SKIP_METHOD, 0);
    }
    
    private static JsonObject readSettings() {
        try {
            return Main.json.fromJson(Files.readString(Path.of("settings.json")), JsonObject.class);
        } catch (IOException e) {
            System.out.println("No settings file, using default settings!");
            return JsonValue.EMPTY_JSON_OBJECT;
        }
    }
    
    public static void saveSettings(JsonObject settings) {
        try {
            Files.writeString(Path.of("settings.json"), Main.json.toJson(settings));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Settings() {}
}