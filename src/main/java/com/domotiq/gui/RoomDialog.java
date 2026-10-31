package com.domotiq.gui;

import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;

public class RoomDialog extends JDialog {

    private final MainFrame main;
    private final Room existing; // null while adding

    private final JTextField nameField = Theme.textField(null, 22);
    private final JTextField descriptionField = Theme.textField(null, 22);

    public RoomDialog(MainFrame main, Room existing) {
        super(main, existing == null ? "Add room" : "Edit room", true);
        this.main = main;
        this.existing = existing;
        buildUi();
        setSize(new Dimension(420, 300));
        setLocationRelativeTo(main);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        if (existing != null) {
            nameField.setText(existing.getName());
            descriptionField.setText(existing.getDescription());
        }

        JPanel form = new JPanel(new BorderLayout(0, 12));
        form.setOpaque(false);
        form.add(captioned("ROOM NAME", nameField), BorderLayout.NORTH);
        form.add(captioned("DESCRIPTION (optional)", descriptionField), BorderLayout.CENTER);

        JPanel buttons = Theme.hRow(10);
        var save = Theme.button("Save", Theme.ACCENT);
        var cancel = Theme.ghostButton("Cancel", Theme.TEXT_MUTED);
        buttons.add(save);
        buttons.add(cancel);

        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> save());
    }

    private void save() {
        try {
            if (existing == null) {
                main.getRoomController().addRoom(
                        main.getUser().getId(), nameField.getText(), descriptionField.getText());
            } else {
                existing.setName(nameField.getText());
                existing.setDescription(descriptionField.getText());
                main.getRoomController().updateRoom(existing);
            }
            main.onDataChanged();
            dispose();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "DOMOTIQ",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JPanel captioned(String caption, JTextField editor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }
}
