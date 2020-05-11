package degubi;

import java.awt.*;
import javax.swing.*;

public final class Components {
    private Components() {}
    
    public static JLabel newLabel(int x, int y, String text) {
        var label = new JLabel(text);
        label.setBounds(x, y, text.length() * 7, 30);
        return label;
    }
    
    public static JCheckBox newCheckBox(int x, int y, String text, boolean selected) {
        var check = new JCheckBox(text, selected);
        check.setBounds(x, y, text.length() * 6, 30);
        check.setFocusPainted(false);
        return check;
    }
    
    public static JRadioButton newRadioButton(int x, int y, int width, String text, int index, boolean selected, ButtonGroup group) {
        var butt = new JRadioButton(text, selected);
        butt.setBounds(x, y, width, 30);
        butt.setMnemonic(index);
        butt.setFocusPainted(false);
        group.add(butt);
        return butt;
    }
    
    @SafeVarargs
    public static<T> JComboBox<T> newComboBox(int x, int y, int width, T settingsValue, T... elements) {
        var wombocombo = new JComboBox<>(elements);
        wombocombo.setBounds(x, y, width, 30);
        wombocombo.setFocusable(false);
        wombocombo.setSelectedItem(settingsValue);
        return wombocombo;
    }
    
    public static void addSettingsSection(String text, int y, JPanel contentPanel, Font font) {
        var label = new JLabel(text);
        label.setBounds(20, y, text.length() * 12, 30);
        label.setFont(font);
        contentPanel.add(label);
        
        var separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setBounds(0, y + 30, 600, 2);
        contentPanel.add(separator);
    }
}