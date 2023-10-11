package degubi;

import java.io.*;
import java.nio.file.*;
import jakarta.json.*;

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
    public static final String SETTING_OUTPUT_DIRECTORY_METHOD = "outputDirectoryMethod";
    public static final String SETTING_OUTPUT_DIRECTORY_CUSTOM = "customOutputDirectory";
    public static final String SETTING_CONTEXT_MENU_OPTION_ENABLED = "contextMenuOptionEnabled";
    public static final String SETTING_VERSION_CHECKING_DISABLED = "versionCheckingDisabled";

    public static final boolean versionCheckingDisabled;
    public static final boolean contextMenuOptionEnabled;
    public static final int rowsPerPage;
    public static final int rowComparisonMethod;
    public static final int columnsPerPage;
    public static final int columnComparisonMethod;
    public static final boolean parallelExtraction;
    public static final boolean autosizeColumns;
    public static final int pageNamingMethod;
    public static final int emptyColumnSkipMethod;
    public static final int outputDirectoryMehod;
    public static final String userSelectedOutputDirectory;
    public static final int emptyRowSkipMethod;

    static {
        var settings = readSettings();

        versionCheckingDisabled = settings.getBoolean(SETTING_VERSION_CHECKING_DISABLED, true);
        rowsPerPage = settings.getInt(SETTING_ROWS_PER_PAGE, 1);
        rowComparisonMethod = settings.getInt(SETTING_ROW_COMPARISON_METHOD, 2);
        columnsPerPage = settings.getInt(SETTING_COLUMNS_PER_PAGE, 1);
        columnComparisonMethod = settings.getInt(SETTING_COLUMN_COMPARISON_METHOD, 2);
        parallelExtraction = settings.getBoolean(SETTING_PARALLEL_FILEPROCESS, false);
        autosizeColumns = settings.getBoolean(SETTING_AUTOSIZE_COLUMNS, true);
        pageNamingMethod = settings.getInt(SETTING_PAGENAMING_METHOD, 0);
        emptyColumnSkipMethod = settings.getInt(SETTING_EMPTY_COLUMN_SKIP_METHOD, 0);
        outputDirectoryMehod = settings.getInt(SETTING_OUTPUT_DIRECTORY_METHOD, 0);
        emptyRowSkipMethod = settings.getInt(SETTING_EMPTY_ROW_SKIP_METHOD, 0);
        userSelectedOutputDirectory = settings.getString(SETTING_OUTPUT_DIRECTORY_CUSTOM, "");
        contextMenuOptionEnabled = settings.getBoolean(SETTING_CONTEXT_MENU_OPTION_ENABLED, false);
    }

    private static JsonObject readSettings() {
        var settingsFilePath = Path.of("settings.json");

        try {
            return Main.json.fromJson(Files.readString(settingsFilePath), JsonObject.class);
        } catch (IOException e) {
            System.out.println("No settings file, using default settings!");

            try {
                Files.writeString(settingsFilePath, "{}");
            } catch (IOException e1) {}

            return JsonValue.EMPTY_JSON_OBJECT;
        }
    }

    public static void save(JsonObject settings, boolean createContextMenu) {
        try {
            Files.writeString(Path.of("settings.json"), Main.json.toJson(settings));

            var regFileTemplate = createContextMenu ? ADD_CONTEXT_TEMPLATE : REMOVE_CONTEXT_TEMPLATE;
            var workDir = Path.of("").toAbsolutePath().toString().replace("\\", "\\\\");
            var regFilePath = Path.of("regToRun.reg").toAbsolutePath();

            Files.writeString(regFilePath, regFileTemplate.replace("%W", workDir));

            try {
                Runtime.getRuntime().exec("cmd /c regedit.exe /S " + regFilePath).waitFor();
                Files.delete(regFilePath);
            }catch (IOException e) {
                System.out.println("Unable to add/remove context menu... :/\n" +
                                   "To do it manually go to the 'app' folder of the application and run the 'regToRun.reg' file manually");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Settings() {}


    private static final String ADD_CONTEXT_TEMPLATE = """
                                                       Windows Registry Editor Version 5.00
                                                       [HKEY_CLASSES_ROOT\\SystemFileAssociations\\.pdf\\shell\\PDFTableExtractor]
                                                       @="Extract Tables to Excel"
                                                       "Icon"="\\"%W\\\\PDFTableExtractor.ico\\""

                                                       [HKEY_CLASSES_ROOT\\SystemFileAssociations\\.pdf\\shell\\PDFTableExtractor\\Command]
                                                       @="cmd /c cd \\"%W\\" && \\"%W\\\\PDFTableExtractor.exe\\" \\"%1\\""
                                                       """;

    private static final String REMOVE_CONTEXT_TEMPLATE = """
                                                          Windows Registry Editor Version 5.00
                                                          [-HKEY_CLASSES_ROOT\\SystemFileAssociations\\.pdf\\shell\\PDFTableExtractor]
                                                          """;
}