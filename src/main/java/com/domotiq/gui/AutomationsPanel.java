package com.domotiq.gui;

import com.domotiq.automation.RuleEngine;
import com.domotiq.controller.AutomationController;
import com.domotiq.model.Automation;
import com.domotiq.model.Device;
import com.domotiq.model.TriggerType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lists the user's scenes and automations as cards. Enable/disable, run
 * (scenes), edit and delete all act through the controllers and keep the
 * shared rule engine in sync.
 */
public class AutomationsPanel extends JPanel {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private final MainFrame main;
    private final JPanel list = new JPanel();

    public AutomationsPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(Theme.label("Scenes & automations", 17, Font.BOLD, Theme.TEXT),
                BorderLayout.WEST);
        JPanel buttons = Theme.hRow(10);
        JButton sceneBtn = Theme.button("+ New scene", Theme.AMBER);
        JButton ruleBtn = Theme.button("+ New automation", Theme.GREEN);
        buttons.add(sceneBtn);
        buttons.add(ruleBtn);
        toolbar.add(buttons, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        list.setOpaque(false);
        list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel note = Theme.hRow(6);
        note.add(Theme.label("Automated actions run on simulated device events and "
                + "are marked AUTOMATION in the activity history.", 11, Font.ITALIC,
                Theme.TEXT_MUTED));
        add(note, BorderLayout.SOUTH);

        sceneBtn.addActionListener(e -> openEditor(true));
        ruleBtn.addActionListener(e -> openEditor(false));
    }

    private void openEditor(boolean sceneShortcut) {
        Automation draft = new Automation();
        draft.setName("");
        draft.setTriggerType(sceneShortcut ? TriggerType.SCENE : TriggerType.SCHEDULE);
        AutomationDialog dialog = new AutomationDialog(
                main, draft, sceneShortcut ? "New scene" : "New automation");
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            main.reloadAutomations();
            refresh();
            main.onDataChanged();
        }
    }

    public void refresh() {
        list.removeAll();
        List<Automation> rules = main.getRuleEngine().getRules();
        if (rules.isEmpty()) {
            JPanel empty = Theme.card();
            empty.setPreferredSize(new Dimension(0, 90));
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.add(Theme.label("No scenes yet. Create a scene (manual) or an "
                    + "automation (schedule / sensor rule).", 13, Font.ITALIC,
                    Theme.TEXT_MUTED));
            list.add(empty);
        }
        for (Automation a : rules) {
            list.add(ruleCard(a));
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel ruleCard(Automation a) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(0, 118));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 12, 0),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel name = Theme.label(a.getName(), 15, Font.BOLD, Theme.TEXT);
        top.add(name, BorderLayout.WEST);
        JPanel right = Theme.hRow(8);

        JCheckBox enabled = new JCheckBox("Enabled", a.isEnabled());
        enabled.setOpaque(false);
        enabled.setForeground(a.isEnabled() ? Theme.GREEN : Theme.TEXT_MUTED);
        enabled.setFont(Theme.font(12, Font.BOLD));
        enabled.addActionListener(e -> toggleEnabled(a, enabled.isSelected(), enabled));
        right.add(enabled);

        if (a.getTriggerType() == TriggerType.SCENE) {
            JButton run = Theme.button("Run now", Theme.ACCENT);
            run.setFont(Theme.font(12, Font.BOLD));
            run.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            run.addActionListener(e -> runScene(a));
            right.add(run);
        }
        JButton edit = Theme.ghostButton("Edit", Theme.TEXT);
        JButton delete = Theme.ghostButton("Delete", Theme.RED);
        for (JButton b : List.of(edit, delete)) {
            b.setFont(Theme.font(12, Font.PLAIN));
            b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            right.add(b);
        }
        edit.addActionListener(e -> {
            AutomationDialog d = new AutomationDialog(main, a, "Edit automation");
            d.setVisible(true);
            if (d.isSaved()) {
                main.reloadAutomations();
                refresh();
            }
        });
        delete.addActionListener(e -> delete(a));
        top.add(right, BorderLayout.EAST);

        String trigger = AutomationController.describeTrigger(a, sensorName(a));
        String steps = describeSteps(a);
        String last = a.getLastFiredAt() == null
                ? "not fired yet" : "last fired " + a.getLastFiredAt().format(TS);
        String meta = trigger + "   ·   " + a.getActions().size() + " step"
                + (a.getActions().size() == 1 ? "" : "s") + "   ·   " + last
                + (a.getTriggerType() != TriggerType.SCENE
                        ? "   ·   cooldown " + a.getCooldownMinutes() + " min" : "");

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
        JLabel triggerLabel = Theme.label(meta, 12, Font.PLAIN,
                a.getTriggerType() == TriggerType.SCENE ? Theme.AMBER : Theme.ACCENT);
        JLabel stepsLabel = Theme.label(steps, 12, Font.PLAIN, Theme.TEXT_MUTED);
        body.add(triggerLabel);
        body.add(stepsLabel);

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void toggleEnabled(Automation a, boolean value, JCheckBox box) {
        try {
            main.getAutomationController().setEnabled(main.getUser().getId(), a, value);
            box.setForeground(value ? Theme.GREEN : Theme.TEXT_MUTED);
        } catch (Exception ex) {
            box.setSelected(!value);
            main.showError("Could not update: " + ex.getMessage());
        }
    }

    private void runScene(Automation a) {
        List<String> problems = main.getRuleEngine().runScene(a.getId());
        main.onDataChanged();
        refresh();
        if (!problems.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Scene ran with warnings:\n• " + String.join("\n• ", problems),
                    "DOMOTIQ", JOptionPane.WARNING_MESSAGE);
        } else {
            main.showInfo("Scene \"" + a.getName() + "\" executed "
                    + a.getActions().size() + " step(s).");
        }
    }

    private void delete(Automation a) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete automation \"" + a.getName() + "\"?", "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_NO_OPTION) {
            return;
        }
        try {
            main.getAutomationController().delete(main.getUser().getId(), a);
            main.reloadAutomations();
            refresh();
        } catch (Exception ex) {
            main.showError("Could not delete: " + ex.getMessage());
        }
    }

    private String sensorName(Automation a) {
        if (a.getSensorDeviceId() == null) {
            return "sensor";
        }
        return main.getDeviceController().getDevices().stream()
                .filter(d -> d.getId() == a.getSensorDeviceId())
                .map(Device::getName).findFirst().orElse("removed sensor");
    }

    private String describeSteps(Automation a) {
        StringBuilder sb = new StringBuilder();
        for (var act : a.getActions()) {
            if (sb.length() > 0) {
                sb.append("  ·  ");
            }
            String dev = main.getDeviceController().getDevices().stream()
                    .filter(d -> d.getId() == act.getDeviceId())
                    .map(Device::getName).findFirst().orElse("(deleted device)");
            sb.append(dev).append(" ");
            sb.append(act.getKind() == com.domotiq.model.AutomationAction.Kind.POWER
                    ? (Boolean.TRUE.equals(act.getPowerTarget()) ? "ON" : "OFF")
                    : act.getSettingKey() + "=" + act.getSettingValue());
        }
        return sb.toString();
    }
}
