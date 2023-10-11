package pdftableextractordesktop;

import jakarta.json.*;

import static pdftableextractordesktop.Settings.*;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.stream.*;
import javax.swing.*;

public final class Components {
    private Components() {}

    private static String cachedUserSelectedXlsDirectory;

    @SuppressWarnings("boxing")
    public static JPanel createSettingsPanel() {
        var oneThruTen = IntStream.rangeClosed(1, 10).boxed().toArray(Integer[]::new);
        var comparisonMethods = new String[] { "less/equal", "equal", "greater/equal" };

        var rowsPerPageSelector = newComboBox(360, 50, 50, rowsPerPage, oneThruTen);
        var rowComparisonSelector = newComboBox(200, 50, 100, comparisonMethods[rowComparisonMethod], comparisonMethods);
        var columnsPerPageSelector = newComboBox(360, 90, 50, columnsPerPage, oneThruTen);
        var columnComparisonSelector = newComboBox(200, 90, 100, comparisonMethods[columnComparisonMethod], comparisonMethods);

        var bigBaldFont = new Font("SansSerif", Font.BOLD, 20);
        var emptyColumnSkipGroup = new ButtonGroup();
        var emptyRowSkipGroup = new ButtonGroup();
        var panel = new JPanel(null);

        addSettingsSection("Page Filters", 10, panel, bigBaldFont);
        panel.add(newLabel(20, 50, "Keep pages with number of rows"));
        panel.add(rowsPerPageSelector);
        panel.add(newLabel(310, 50, "than/to"));
        panel.add(rowComparisonSelector);
        panel.add(newLabel(20, 90, "Keep pages with number of columns"));
        panel.add(columnsPerPageSelector);
        panel.add(newLabel(310, 90, "than/to"));
        panel.add(columnComparisonSelector);
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

        var pageNamingMethods = new String[] { "Counting", "PageOrdinal - TableOrdinal" };
        var pageNamingComboBox = newComboBox(140, 240, 150, pageNamingMethods[pageNamingMethod], pageNamingMethods);
        var autosizeColumnsCheckBox = newCheckBox(15, 280, "Autosize columns after extraction", autosizeColumns);

        addSettingsSection("Page Output", 200, panel, bigBaldFont);
        panel.add(newLabel(20, 240, "Page naming strategy: "));
        panel.add(pageNamingComboBox);
        panel.add(autosizeColumnsCheckBox);

        var outputDirectoryGroup = new ButtonGroup();
        var outDirPdfDirOption = newRadioButton(100, 360, 120, "Input .pdf directory", 0, outputDirectoryMehod == 0, outputDirectoryGroup);
        var outDirSelectOption = newRadioButton(250, 360, 150, "Select before extraction", 1, outputDirectoryMehod == 1, outputDirectoryGroup);
        var outDirCustomOption = newRadioButton(100, 390, 150, "Predefined directory", 2, outputDirectoryMehod == 2, outputDirectoryGroup);
        var customOutDirField = newTextField(Settings.userSelectedOutputDirectory, 255, 390, 250);
        var customOutDirChooserButton = newButton("...", 520, 385, 40, e -> customOutDirField.setText(showDirectorySelection(customOutDirField.getText())));

        outDirPdfDirOption.addActionListener(e -> handleOutDirOptionChange(false, customOutDirField, customOutDirChooserButton));
        outDirSelectOption.addActionListener(e -> handleOutDirOptionChange(false, customOutDirField, customOutDirChooserButton));
        outDirCustomOption.addActionListener(e -> handleOutDirOptionChange(true, customOutDirField, customOutDirChooserButton));

        handleOutDirOptionChange(outputDirectoryMehod == 2, customOutDirField, customOutDirChooserButton);

        addSettingsSection("File Output", 320, panel, bigBaldFont);
        panel.add(newLabel(20, 360, "Output Path:"));
        panel.add(outDirPdfDirOption);
        panel.add(outDirSelectOption);
        panel.add(outDirCustomOption);
        panel.add(customOutDirField);
        panel.add(customOutDirChooserButton);

        var parallelCheckBox = newCheckBox(15, 470, "Enable parallel file processing", parallelExtraction);
        var pdfContextMenuCheckBox = newCheckBox(15, 500, "Enable PDF extraction context menu", contextMenuOptionEnabled);
        var versionCheckingDisabledBox = newCheckBox(15, 530, "Disable version checking", versionCheckingDisabled);
        versionCheckingDisabledBox.setEnabled(Settings.isWindows());

        addSettingsSection("App Settings", 430, panel, bigBaldFont);
        panel.add(parallelCheckBox);
        panel.add(pdfContextMenuCheckBox);
        panel.add(versionCheckingDisabledBox);

        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   var userSelectedDirectoryString = customOutDirField.getText();
                   var userSelectedDirectoryMethod = outputDirectoryGroup.getSelection().getMnemonic();
                   var isUserDirectoryValid = !userSelectedDirectoryString.isBlank() && Files.isDirectory(Path.of(userSelectedDirectoryString));

                   var settings = Json.createObjectBuilder()
                                      .add(SETTING_ROWS_PER_PAGE, (int) rowsPerPageSelector.getSelectedItem())
                                      .add(SETTING_ROW_COMPARISON_METHOD, indexOf((String) rowComparisonSelector.getSelectedItem(), comparisonMethods))
                                      .add(SETTING_COLUMNS_PER_PAGE, (int) columnsPerPageSelector.getSelectedItem())
                                      .add(SETTING_COLUMN_COMPARISON_METHOD, indexOf((String) columnComparisonSelector.getSelectedItem(), comparisonMethods))
                                      .add(SETTING_AUTOSIZE_COLUMNS, autosizeColumnsCheckBox.isSelected())
                                      .add(SETTING_PARALLEL_FILEPROCESS, parallelCheckBox.isSelected())
                                      .add(SETTING_PAGENAMING_METHOD, indexOf((String) pageNamingComboBox.getSelectedItem(), pageNamingMethods))
                                      .add(SETTING_EMPTY_COLUMN_SKIP_METHOD, emptyColumnSkipGroup.getSelection().getMnemonic())
                                      .add(SETTING_EMPTY_ROW_SKIP_METHOD, emptyRowSkipGroup.getSelection().getMnemonic())
                                      .add(SETTING_OUTPUT_DIRECTORY_METHOD, userSelectedDirectoryMethod != 2 ? userSelectedDirectoryMethod : isUserDirectoryValid ? 2 : 0)
                                      .add(SETTING_OUTPUT_DIRECTORY_CUSTOM, isUserDirectoryValid ? userSelectedDirectoryString : "")
                                      .add(SETTING_VERSION_CHECKING_DISABLED, versionCheckingDisabledBox.isSelected())
                                      .add(SETTING_CONTEXT_MENU_OPTION_ENABLED, pdfContextMenuCheckBox.isSelected())
                                      .build();

                   Settings.save(settings, pdfContextMenuCheckBox.isSelected());
               }));

        return panel;
    }

    public static String showDirectorySelection(String rootDir) {
        var fileChooser = new JFileChooser(rootDir);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setPreferredSize(new Dimension(1300, 700));

        if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().toPath().toAbsolutePath().toString();
        }

        return "";
    }

    public static String showExcelDirectoryPicker(String rootDir) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}

        if(cachedUserSelectedXlsDirectory == null) {
            cachedUserSelectedXlsDirectory = showDirectorySelection(rootDir);
        }

        return cachedUserSelectedXlsDirectory;
    }


    private static void handleOutDirOptionChange(boolean enabled, JTextField customOutDirField, JButton customOutDirChooserButton) {
        customOutDirField.setEnabled(enabled);
        customOutDirChooserButton.setEnabled(enabled);
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

    private static JTextField newTextField(String defaultValue, int x, int y, int width) {
        var field = new JTextField(defaultValue);
        field.setBounds(x, y, width, 30);
        return field;
    }

    private static JButton newButton(String text, int x, int y, int width, ActionListener listener) {
        var butt = new JButton(text);
        butt.setBounds(x, y, width, 40);
        butt.setBackground(Color.DARK_GRAY);
        butt.setFocusPainted(false);
        butt.addActionListener(listener);
        return butt;
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


    private static<T> int indexOf(T element, T[] array) {
        for(var i = 0; i < array.length; ++i) {
            if(array[i].equals(element)) {
                return i;
            }
        }
        return -1;
    }
}