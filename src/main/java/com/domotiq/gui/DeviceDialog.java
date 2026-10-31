package com.domotiq.gui;

import com.domotiq.controller.DeviceController;
import com.domotiq.factory.DeviceFactory;
import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Add/edit dialog. Category controls are generated from settingDefinitions(). */
public class DeviceDialog extends JDialog {

    private final MainFrame main;
    private final Device device; // null while adding
    private final boolean adding;

    private final JTextField nameField = Theme.textField(null, 24);
    private final JComboBox<DeviceType> typeCombo = new JComboBox<>(DeviceType.values());
    private final JComboBox<String> roomCombo = new JComboBox<>();
    private final JPanel settingsPanel = new JPanel(new GridLayout(0, 1, 0, 10));
    private final Map<String, JComponent> settingEditors = new HashMap<>();

    private List<Room> rooms;

    public DeviceDialog(MainFrame main, Device device, boolean adding) {
        super(main, adding ? "Add device" : "Manage " + device.getName(), true);
        this.main = main;
        this.device = device;
        this.adding = adding;
        try {
            rooms = main.getRoomController().listRooms(main.getUser().getId());
        } catch (SQLException ex) {
            rooms = List.of();
            main.showError("Could not load rooms: " + ex.getMessage());
        }
        buildUi();
        pack();
        setMinimumSize(new Dimension(480, getHeight()));
        setLocationRelativeTo(main);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel basics = new JPanel(new GridLayout(0, 1, 0, 10));
        basics.setOpaque(false);

        roomCombo.addItem("— Unassigned —");
        rooms.forEach(r -> roomCombo.addItem(r.getName()));
        styleInput(typeCombo);
        styleInput(roomCombo);

        if (adding) {
            basics.add(captioned("DEVICE NAME", nameField));
            basics.add(captioned("CATEGORY", typeCombo));
            basics.add(captioned("ROOM", roomCombo));
        } else {
            nameField.setText(device.getName());
            typeCombo.setSelectedItem(device.getDeviceType());
            typeCombo.setEnabled(false);
            if (device.getRoomId() != null) {
                for (int i = 0; i < rooms.size(); i++) {
                    if (rooms.get(i).getId() == device.getRoomId()) {
                        roomCombo.setSelectedIndex(i + 1);
                    }
                }
            }
            basics.add(captioned("DEVICE NAME", nameField));
            basics.add(captioned("CATEGORY (cannot change after creation)", typeCombo));
            basics.add(captioned("ROOM", roomCombo));
        }

        settingsPanel.setOpaque(false);
        JPanel settingsWrap = new JPanel(new BorderLayout(0, 8));
        settingsWrap.setOpaque(false);
        settingsWrap.add(Theme.label("CONTROLS", 11, Font.BOLD, Theme.TEXT_MUTED),
                BorderLayout.NORTH);
        settingsWrap.add(settingsPanel, BorderLayout.CENTER);

        root.add(basics, BorderLayout.NORTH);
        root.add(settingsWrap, BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(root);

        typeCombo.addActionListener(e -> rebuildSettings());
        rebuildSettings();
    }

    private void rebuildSettings() {
        settingsPanel.removeAll();
        settingEditors.clear();
        DeviceType type = (DeviceType) typeCombo.getSelectedItem();
        if (type == null) {
            return;
        }
        Device template = adding
                ? DeviceFactory.create(type)
                : device;
        Map<String, String> current = adding ? template.getSettings() : device.getSettings();
        for (Device.SettingDefinition def : template.settingDefinitions()) {
            JComponent editor = createEditor(def, current.getOrDefault(def.key, ""));
            settingEditors.put(def.key, editor);
            settingsPanel.add(captioned(def.label.toUpperCase(), editor));
        }
        settingsPanel.revalidate();
        settingsPanel.repaint();
        pack();
    }

    private JComponent createEditor(Device.SettingDefinition def, String value) {
        switch (def.kind) {
            case SLIDER -> {
                JSlider slider = new JSlider(def.min, def.max);
                slider.setValue(parseIntSafe(value, def.min));
                slider.setMajorTickSpacing(Math.max(1, (def.max - def.min) / 4));
                slider.setPaintTicks(true);
                slider.setPaintLabels(true);
                slider.setFont(Theme.font(12, Font.PLAIN));
                slider.setForeground(Theme.TEXT);
                slider.setBackground(Theme.PANEL);
                return slider;
            }
            case COMBO -> {
                JComboBox<String> combo = new JComboBox<>(def.options.toArray(String[]::new));
                combo.setSelectedItem(value);
                styleInput(combo);
                return combo;
            }
            case TOGGLE -> {
                javax.swing.JCheckBox check = new javax.swing.JCheckBox("enabled");
                check.setSelected(Boolean.parseBoolean(value));
                check.setOpaque(false);
                check.setForeground(Theme.TEXT);
                check.setFont(Theme.font(13, Font.PLAIN));
                return check;
            }
            default -> throw new IllegalStateException("Unknown control " + def.kind);
        }
    }

    private JPanel buildButtons() {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);

        var save = Theme.button(adding ? "Add device" : "Save changes", Theme.ACCENT);
        save.addActionListener(e -> save());
        row.add(save, BorderLayout.NORTH);

        JPanel secondary = Theme.hRow(10);
        secondary.add(makeCancel());
        if (!adding) {
            var connection = Theme.ghostButton(
                    device.isOnline() ? "Simulate offline" : "Simulate online", Theme.AMBER);
            connection.addActionListener(e -> {
                try {
                    main.getDeviceController().setConnection(device, !device.isOnline());
                    main.onDataChanged();
                    dispose();
                } catch (SQLException ex) {
                    main.showError(ex.getMessage());
                }
            });
            secondary.add(connection);

            var delete = Theme.button("Delete", Theme.RED);
            delete.addActionListener(e -> confirmDelete());
            secondary.add(delete);
        }
        row.add(secondary, BorderLayout.SOUTH);
        return row;
    }

    private javax.swing.JButton makeCancel() {
        var cancel = Theme.ghostButton("Cancel", Theme.TEXT_MUTED);
        cancel.addActionListener(e -> dispose());
        return cancel;
    }

    private void save() {
        try {
            DeviceController controller = main.getDeviceController();
            Map<String, String> values = collectSettingValues();
            Integer roomId = roomCombo.getSelectedIndex() == 0
                    ? null : rooms.get(roomCombo.getSelectedIndex() - 1).getId();
            if (adding) {
                controller.addDevice(nameField.getText(),
                        (DeviceType) typeCombo.getSelectedItem(), roomId, values);
            } else {
                controller.updateDevice(device, nameField.getText(), roomId, values);
            }
            main.onDataChanged();
            dispose();
        } catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
            main.showError(ex.getMessage());
        }
    }

    private Map<String, String> collectSettingValues() {
        Map<String, String> values = new HashMap<>();
        settingEditors.forEach((key, editor) -> {
            if (editor instanceof JSlider slider) {
                values.put(key, String.valueOf(slider.getValue()));
            } else if (editor instanceof JComboBox<?> combo) {
                values.put(key, String.valueOf(combo.getSelectedItem()));
            } else if (editor instanceof javax.swing.JCheckBox check) {
                values.put(key, String.valueOf(check.isSelected()));
            }
        });
        return values;
    }

    private void confirmDelete() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete \"" + device.getName() + "\" permanently?\n"
                        + "Its settings will be removed; history entries are kept.",
                "Delete device", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                main.getDeviceController().deleteDevice(device.getId());
                main.onDataChanged();
                dispose();
            } catch (SQLException ex) {
                main.showError(ex.getMessage());
            }
        }
    }

    private static void styleInput(JComponent c) {
        c.setFont(Theme.font(13, Font.PLAIN));
        c.setForeground(Theme.TEXT);
        c.setBackground(Theme.FIELD_BG);
        c.setBorder(BorderFactory.createLineBorder(Theme.INPUT_BORDER));
    }

    private static JPanel captioned(String caption, JComponent editor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
