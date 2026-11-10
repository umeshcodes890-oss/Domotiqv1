package com.domotiq.gui;

import com.domotiq.model.Automation;
import com.domotiq.model.AutomationAction;
import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.model.TriggerType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder dialog for one automation/scene: name, trigger (scene, schedule or
 * sensor threshold) and an ordered list of device actions. Editing an existing
 * rule pre-fills everything.
 */
public class AutomationDialog extends JDialog {

    private static final String CARD_SCENE = "scene";
    private static final String CARD_SCHEDULE = "schedule";
    private static final String CARD_SENSOR = "sensor";
    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final MainFrame main;
    private final Automation working;
    private boolean saved;

    private final JTextField nameField = Theme.textField("e.g. Good night", 24);
    private final JComboBox<String> typeCombo = new JComboBox<>();
    private final JPanel conditionCards = new JPanel(new CardLayout());

    // schedule widgets
    private final JTextField fireTimeField = Theme.textField("22:30", 6);
    private final JCheckBox[] dayChecks = new JCheckBox[7];
    private final JCheckBox windowCheck = new JCheckBox("Only fire inside a time window");
    private final JTextField windowStart = Theme.textField("07:00", 6);
    private final JTextField windowEnd = Theme.textField("23:00", 6);
    private final JTextField scheduleCooldown = Theme.textField("10", 4);

    // sensor widgets
    private final JComboBox<Device> sensorCombo = new JComboBox<>();
    private final JComboBox<Automation.SensorOp> opCombo =
            new JComboBox<>(Automation.SensorOp.values());
    private final JTextField sensorValueField = Theme.textField("28", 6);
    private final JLabel sensorValueCaption = Theme.label("Threshold value",
            11, Font.BOLD, Theme.TEXT_MUTED);
    private final JTextField sensorCooldown = Theme.textField("10", 4);

    // action builder
    private final JComboBox<Device> actionDeviceCombo = new JComboBox<>();
    private final JComboBox<String> actionKindCombo = new JComboBox<>(
            new String[] {"Turn ON", "Turn OFF", "Set a setting..."});
    private final JComboBox<Device.SettingDefinition> settingCombo = new JComboBox<>();
    private final JPanel valueCard = new JPanel(new CardLayout());
    private final JTextField sliderValueField = Theme.textField("value", 6);
    private final JComboBox<String> optionValueCombo = new JComboBox<>();
    private final JTextField toggleValueField = Theme.textField("true", 6);
    private final JPanel actionList = new JPanel();
    private final List<AutomationAction> actions = new ArrayList<>();

    private List<AutomationAction> uiActions = new ArrayList<>();

    public AutomationDialog(MainFrame main, Automation existing, String title) {
        super(main, title, true);
        this.main = main;
        this.working = existing == null ? new Automation() : existing;
        this.uiActions = new ArrayList<>();

        setMinimumSize(new Dimension(680, 720));
        setSize(new Dimension(680, 760));
        setLocationRelativeTo(main);
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 8, 22));

        form.add(section("Automation name", nameField));
        form.add(strut(10));
        form.add(section("Trigger", typeCombo));

        typeCombo.addItem(TriggerType.SCENE.getLabel());
        typeCombo.addItem(TriggerType.SCHEDULE.getLabel());
        typeCombo.addItem(TriggerType.SENSOR.getLabel());
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        conditionCards.setOpaque(false);
        conditionCards.add(buildSceneCard(), CARD_SCENE);
        conditionCards.add(buildScheduleCard(), CARD_SCHEDULE);
        conditionCards.add(buildSensorCard(), CARD_SENSOR);
        conditionCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(conditionCards);
        form.add(strut(14));

        form.add(buildActionsSection());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel footer = Theme.hRow(12);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 22, 16, 22));
        JButton saveBtn = Theme.button("Save automation", Theme.GREEN);
        JButton cancelBtn = Theme.ghostButton("Cancel", Theme.TEXT_MUTED);
        footer.add(saveBtn);
        footer.add(cancelBtn);
        add(footer, BorderLayout.SOUTH);

        typeCombo.addActionListener(e -> showConditionCard());
        actionDeviceCombo.addActionListener(e -> refreshSettingCombo());
        actionKindCombo.addActionListener(e -> refreshValueEditor());
        windowCheck.addActionListener(e -> {
            windowStart.setEnabled(windowCheck.isSelected());
            windowEnd.setEnabled(windowCheck.isSelected());
        });
        opCombo.addActionListener(e -> {
            boolean detected = opCombo.getSelectedItem() == Automation.SensorOp.DETECTED;
            sensorValueField.setEnabled(!detected);
            sensorValueCaption.setText(detected
                    ? "No value needed - fires when the sensor reports detection"
                    : "Threshold value");
        });
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());

        prefill();
        showConditionCard();
        rebuildActionList();
    }

    /* ---------- layout helpers ---------- */

    private Component strut(int h) {
        return javax.swing.Box.createVerticalStrut(h);
    }

    private JPanel section(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        style(field);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static void style(JComponent c) {
        c.setFont(Theme.font(13, Font.PLAIN));
        c.setForeground(Theme.TEXT);
        if (c instanceof JComboBox<?> cb) {
            cb.setBackground(Theme.FIELD_BG);
            return;
        }
        c.setBackground(Theme.FIELD_BG);
    }

    private JPanel buildSceneCard() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout());
        p.add(Theme.label("<html>A scene runs its steps only when you press Run.<br>"
                + "Nothing triggers it automatically.</html>", 12, Font.ITALIC,
                Theme.TEXT_MUTED), BorderLayout.NORTH);
        return p;
    }

    private JPanel buildScheduleCard() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout(0, 8));

        JPanel row1 = Theme.hRow(8);
        row1.add(label13("Fire at (HH:mm)"));
        row1.add(fireTimeField);
        row1.add(label13("Cooldown (min)"));
        row1.add(scheduleCooldown);

        JPanel days = Theme.hRow(2);
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new JCheckBox(DAY_NAMES[i]);
            dayChecks[i].setOpaque(false);
            dayChecks[i].setForeground(Theme.TEXT);
            dayChecks[i].setSelected(true);
            days.add(dayChecks[i]);
        }

        JPanel row3 = Theme.hRow(8);
        windowCheck.setOpaque(false);
        windowCheck.setForeground(Theme.TEXT);
        windowStart.setEnabled(false);
        windowEnd.setEnabled(false);
        row3.add(windowCheck);
        row3.add(windowStart);
        row3.add(label13("to"));
        row3.add(windowEnd);

        p.add(row1, BorderLayout.NORTH);
        p.add(days, BorderLayout.CENTER);
        p.add(row3, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildSensorCard() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout(0, 8));

        JPanel row1 = Theme.hRow(8);
        row1.add(label13("Watch sensor"));
        sensorCombo.setRenderer(new ListCellWrap<Device>(
                d -> d == null ? "" : d.getName() + " ("
                        + d.getSettings().getOrDefault(
                        com.domotiq.model.devices.SmartSensor.SENSOR_KIND, "TEMPERATURE")
                        + ")"));
        sensorCombo.setPreferredSize(new Dimension(260, 36));
        row1.add(sensorCombo);

        JPanel row2 = Theme.hRow(8);
        row2.add(label13("When reading"));
        opCombo.setPreferredSize(new Dimension(160, 36));
        opCombo.setRenderer(new ListCellWrap<Automation.SensorOp>(
                op -> op == null ? "" : op.getLabel()));
        row2.add(opCombo);
        JPanel valueBox = new JPanel(new BorderLayout(0, 4));
        valueBox.setOpaque(false);
        valueBox.add(sensorValueCaption, BorderLayout.NORTH);
        sensorValueField.setPreferredSize(new Dimension(90, 36));
        valueBox.add(sensorValueField, BorderLayout.CENTER);
        row2.add(valueBox);
        row2.add(label13("Cooldown (min)"));
        row2.add(sensorCooldown);

        p.add(row1, BorderLayout.NORTH);
        p.add(row2, BorderLayout.CENTER);
        p.add(Theme.label("Readings are simulated telemetry; a rule can watch one sensor.",
                11, Font.ITALIC, Theme.TEXT_MUTED), BorderLayout.SOUTH);
        return p;
    }

    private JLabel label13(String t) {
        return Theme.label(t, 12, Font.PLAIN, Theme.TEXT);
    }

    private JPanel buildActionsSection() {
        JPanel outer = Theme.card();
        outer.setLayout(new BorderLayout(0, 10));
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(Theme.label("Steps - executed in order", 13, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);

        // two fixed columns: controls always fit the card instead of wrapping off-screen
        JPanel builder = new JPanel(new GridLayout(0, 2, 14, 12));
        builder.setOpaque(false);

        style(actionDeviceCombo);
        actionDeviceCombo.setRenderer(new ListCellWrap<Device>(
                d -> d == null ? "" : d.getName() + "  ·  " + d.getDeviceType().getLabel()));
        style(actionKindCombo);
        style(settingCombo);
        settingCombo.setRenderer(new ListCellWrap<Device.SettingDefinition>(
                d -> d == null ? "" : d.label));
        style(valueCard);
        valueCard.setOpaque(false);
        sliderValueField.setPreferredSize(new Dimension(190, 34));
        optionValueCombo.setPreferredSize(new Dimension(190, 34));
        style(optionValueCombo);
        toggleValueField.setVisible(false);
        valueCard.add(sliderValueField, "slider");
        valueCard.add(optionValueCombo, "option");

        builder.add(labeledField("Device", actionDeviceCombo));
        builder.add(labeledField("Action", actionKindCombo));
        builder.add(labeledField("Setting", settingCombo));
        builder.add(labeledField("Value", valueCard));
        outer.add(builder, BorderLayout.CENTER);

        JButton addBtn = Theme.button("+ Add step", Theme.ACCENT);
        addBtn.addActionListener(e -> addStepFromForm());
        JPanel addRow = Theme.hRow(8);
        addRow.add(addBtn);

        actionList.setOpaque(false);
        actionList.setLayout(new javax.swing.BoxLayout(actionList,
                javax.swing.BoxLayout.Y_AXIS));
        JScrollPane listScroll = new JScrollPane(actionList);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setPreferredSize(new Dimension(0, 120));
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(addRow, BorderLayout.NORTH);
        bottom.add(listScroll, BorderLayout.CENTER);
        outer.add(bottom, BorderLayout.SOUTH);
        return outer;
    }

    /** Caption above a control, filling one column of the steps grid. */
    private static JPanel labeledField(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(200, 36));
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /* ---------- combo contents ---------- */

    private void populateDeviceCombos() {
        List<Device> devices = main.getDeviceController().getDevices();
        for (Device d : devices) {
            if (d.getDeviceType() == DeviceType.SENSOR) {
                sensorCombo.addItem(d);
            }
            actionDeviceCombo.addItem(d);
        }
        if (!devices.isEmpty()) {
            actionDeviceCombo.setSelectedIndex(0);
        }
        if (sensorCombo.getItemCount() > 0) {
            sensorCombo.setSelectedIndex(0);
        }
        refreshSettingCombo();
    }

    private void refreshSettingCombo() {
        settingCombo.removeAllItems();
        Device d = (Device) actionDeviceCombo.getSelectedItem();
        if (d == null) {
            return;
        }
        for (Device.SettingDefinition def : d.settingDefinitions()) {
            settingCombo.addItem(def);
        }
        refreshValueEditor();
    }

    private void refreshValueEditor() {
        Device.SettingDefinition def = (Device.SettingDefinition) settingCombo.getSelectedItem();
        var cl = (CardLayout) valueCard.getLayout();
        settingCombo.setVisible(def != null);
        if (def == null) {
            valueCard.setVisible(false);
            return;
        }
        valueCard.setVisible(true);
        switch (def.kind) {
            case SLIDER -> {
                cl.show(valueCard, "slider");
                sliderValueField.setText(String.valueOf(def.min));
                sliderValueField.setToolTipText(
                        def.min + " - " + def.max);
            }
            case COMBO -> {
                cl.show(valueCard, "option");
                optionValueCombo.removeAllItems();
                def.options.forEach(optionValueCombo::addItem);
                if (!def.options.isEmpty()) {
                    optionValueCombo.setSelectedIndex(0);
                }
            }
            case TOGGLE -> {
                cl.show(valueCard, "option");
                optionValueCombo.removeAllItems();
                optionValueCombo.addItem("true");
                optionValueCombo.addItem("false");
            }
        }
    }

    /* ---------- steps ---------- */

    private void addStepFromForm() {
        Device d = (Device) actionDeviceCombo.getSelectedItem();
        if (d == null) {
            main.showError("Add a device first - automations control devices.");
            return;
        }
        int kind = actionKindCombo.getSelectedIndex();
        try {
            if (kind == 0) {
                uiActions.add(AutomationAction.power(d.getId(), true, uiActions.size()));
            } else if (kind == 1) {
                uiActions.add(AutomationAction.power(d.getId(), false, uiActions.size()));
            } else {
                Device.SettingDefinition def =
                        (Device.SettingDefinition) settingCombo.getSelectedItem();
                if (def == null) {
                    main.showError("This device has no settings to change.");
                    return;
                }
                String value = switch (def.kind) {
                    case SLIDER -> sliderValueField.getText().trim();
                    case COMBO, TOGGLE -> String.valueOf(
                            optionValueCombo.getSelectedItem());
                };
                d.setSetting(def.key, value); // validates range/options
                uiActions.add(AutomationAction.set(d.getId(), def.key, value,
                        uiActions.size()));
            }
            rebuildActionList();
        } catch (RuntimeException bad) {
            JOptionPane.showMessageDialog(this, bad.getMessage(),
                    "Check the step", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void rebuildActionList() {
        actionList.removeAll();
        int i = 0;
        for (AutomationAction act : uiActions) {
            Device d = main.getDeviceController().getDevices().stream()
                    .filter(x -> x.getId() == act.getDeviceId())
                    .findFirst().orElse(null);
            String name = d == null ? "(deleted device)" : d.getName();
            String what;
            if (act.getKind() == AutomationAction.Kind.POWER) {
                what = d != null && d.getDeviceType() == DeviceType.DOOR_LOCK
                        ? (act.getPowerTarget() ? "LOCK" : "UNLOCK")
                        : (act.getPowerTarget() ? "Turn ON" : "Turn OFF");
            } else {
                String label = d == null ? act.getSettingKey() : d.settingDefinitions()
                        .stream().filter(s -> s.key.equals(act.getSettingKey()))
                        .map(s -> s.label).findFirst().orElse(act.getSettingKey());
                what = label + " = " + act.getSettingValue();
            }
            final int index = i++;
            JPanel row = Theme.hRow(8);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            row.add(Theme.label((index + 1) + ".  " + name + "  ->  " + what,
                    12, Font.PLAIN, Theme.TEXT));
            JButton remove = Theme.ghostButton("Remove", Theme.RED);
            remove.setFont(Theme.font(11, Font.PLAIN));
            remove.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            remove.addActionListener(e -> {
                uiActions.remove(index);
                rebuildActionList();
            });
            row.add(remove);
            actionList.add(row);
        }
        actionList.revalidate();
        actionList.repaint();
    }

    /* ---------- prefill + save ---------- */

    private void prefill() {
        populateDeviceCombos();
        if (working.getTriggerType() != null) {
            typeCombo.setSelectedIndex(switch (working.getTriggerType()) {
                case SCENE -> 0;
                case SCHEDULE -> 1;
                case SENSOR -> 2;
            });
        }
        if (working.getId() != -1) {
            nameField.setText(working.getName());
            fireTimeField.setText(working.getFireTime() == null
                    ? "22:30" : String.format("%02d:%02d",
                    working.getFireTime().getHour(), working.getFireTime().getMinute()));
            String mask = working.getDaysMask() == null ? "1111111" : working.getDaysMask();
            for (int i = 0; i < 7; i++) {
                dayChecks[i].setSelected(mask.charAt(i) == '1');
            }
            if (working.getActiveStart() != null && working.getActiveEnd() != null) {
                windowCheck.setSelected(true);
                windowStart.setEnabled(true);
                windowEnd.setEnabled(true);
                windowStart.setText(fmt(working.getActiveStart()));
                windowEnd.setText(fmt(working.getActiveEnd()));
            }
            scheduleCooldown.setText(String.valueOf(working.getCooldownMinutes()));
            sensorCooldown.setText(String.valueOf(working.getCooldownMinutes()));
            if (working.getSensorDeviceId() != null) {
                for (int i = 0; i < sensorCombo.getItemCount(); i++) {
                    if (sensorCombo.getItemAt(i).getId() == working.getSensorDeviceId()) {
                        sensorCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (working.getSensorOp() != null) {
                opCombo.setSelectedItem(working.getSensorOp());
            }
            if (working.getSensorValue() != null) {
                sensorValueField.setText(String.valueOf(working.getSensorValue()));
            }
            uiActions = new ArrayList<>(working.getActions());
        }
    }

    private static String fmt(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private void showConditionCard() {
        String card = switch (typeCombo.getSelectedIndex()) {
            case 1 -> CARD_SCHEDULE;
            case 2 -> CARD_SENSOR;
            default -> CARD_SCENE;
        };
        ((CardLayout) conditionCards.getLayout()).show(conditionCards, card);
        // CardLayout reports the tallest card, so size the strip to the visible one
        int height = 0;
        for (Component c : conditionCards.getComponents()) {
            if (c.isVisible()) {
                height = c.getPreferredSize().height;
            }
        }
        conditionCards.setPreferredSize(new Dimension(0, height));
        conditionCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        revalidate();
    }

    private void save() {
        try {
            Automation a = collect();
            main.getAutomationController().save(main.getUser().getId(), a);
            main.reloadAutomations();
            saved = true;
            dispose();
        } catch (IllegalArgumentException bad) {
            JOptionPane.showMessageDialog(this, bad.getMessage(),
                    "Check the automation", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Automation collect() {
        working.setName(nameField.getText());
        working.setTriggerType(switch (typeCombo.getSelectedIndex()) {
            case 1 -> TriggerType.SCHEDULE;
            case 2 -> TriggerType.SENSOR;
            default -> TriggerType.SCENE;
        });
        working.getActions().clear();
        working.getActions().addAll(uiActions);

        switch (working.getTriggerType()) {
            case SCHEDULE -> {
                working.setFireTime(parseTime(fireTimeField.getText(), "fire time"));
                StringBuilder mask = new StringBuilder();
                for (JCheckBox c : dayChecks) {
                    mask.append(c.isSelected() ? '1' : '0');
                }
                working.setDaysMask(mask.toString());
                if (windowCheck.isSelected()) {
                    working.setActiveStart(parseTime(windowStart.getText(), "window start"));
                    working.setActiveEnd(parseTime(windowEnd.getText(), "window end"));
                } else {
                    working.setActiveStart(null);
                    working.setActiveEnd(null);
                }
                working.setSensorDeviceId(null);
                working.setSensorOp(null);
                working.setSensorValue(null);
                working.setCooldownMinutes(parseInt(scheduleCooldown.getText(), 10,
                        "cooldown minutes"));
            }
            case SENSOR -> {
                Device sensor = (Device) sensorCombo.getSelectedItem();
                working.setSensorDeviceId(sensor == null ? null : sensor.getId());
                Automation.SensorOp op = (Automation.SensorOp) opCombo.getSelectedItem();
                working.setSensorOp(op);
                working.setSensorValue(op == Automation.SensorOp.DETECTED ||
                        sensorValueField.getText().isBlank()
                                ? null : Double.parseDouble(sensorValueField.getText().trim()));
                working.setFireTime(null);
                working.setActiveStart(null);
                working.setActiveEnd(null);
                working.setCooldownMinutes(parseInt(sensorCooldown.getText(), 10,
                        "cooldown minutes"));
            }
            default -> {
                working.setSensorDeviceId(null);
                working.setSensorOp(null);
                working.setSensorValue(null);
                working.setFireTime(null);
                working.setActiveStart(null);
                working.setActiveEnd(null);
                working.setDaysMask("1111111");
                working.setCooldownMinutes(0);
            }
        }
        return working;
    }

    private static LocalTime parseTime(String text, String what) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid " + what + ": " + text
                    + " (use HH:mm like 22:30)");
        }
    }

    private static int parseInt(String text, int fallback, String what) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + what + ": " + text);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
