package com.domotiq.gui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/** Applies the DOMOTIQ dark style to swing tables. */
public final class UiTables {

    private UiTables() {
    }

    public static JTable styledTable(String[] columns, int preferredWidth) {
        JTable table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFillsViewportHeight(true);
        table.setFont(Theme.font(13, Font.PLAIN));
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.PANEL);
        table.setRowHeight(32);
        table.setGridColor(Theme.GRID);
        table.setShowGrid(true);
        table.setSelectionBackground(Theme.ACCENT_DARK);
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.font(12, Font.BOLD));
        header.setBackground(Theme.CARD);
        header.setForeground(Theme.TEXT_MUTED);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(JLabel.LEFT);

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[0][columns.length], columns));
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    l.setBackground(row % 2 == 0 ? Theme.PANEL : Theme.ROW_ALT);
                }
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return l;
            }
        };
        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        if (preferredWidth > 0) {
            table.setPreferredScrollableViewportSize(
                    new java.awt.Dimension(preferredWidth, 240));
        }
        var scroll = table.getParent();
        if (scroll instanceof JViewport vp) {
            vp.setOpaque(false);
            vp.setBackground(Theme.PANEL);
        }
        return table;
    }

    /** Appends a row, returning the model for chaining. */
    public static javax.swing.table.DefaultTableModel modelOf(JTable table) {
        return (javax.swing.table.DefaultTableModel) table.getModel();
    }
}
