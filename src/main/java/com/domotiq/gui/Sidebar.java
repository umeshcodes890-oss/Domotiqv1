package com.domotiq.gui;

import com.domotiq.gui.NavIcons.Kind;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The navigation rail: brand, one entry per screen, and a footer holding the theme
 * switch and the collapse control. Collapsed it keeps the icons and drops the text,
 * and the width change is tweened so the rail never snaps between states.
 */
class Sidebar extends JPanel {

    private static final int WIDTH_EXPANDED = 210;
    private static final int WIDTH_COLLAPSED = 66;
    private static final int SIDE_INSET = 10;
    private static final int NAV_HEIGHT = 46;
    private static final String[] LABELS = {
        "Dashboard", "Devices", "Rooms", "Automations", "Energy", "History"
    };
    private static final Kind[] GLYPHS = {
        Kind.DASHBOARD, Kind.DEVICES, Kind.ROOMS, Kind.AUTOMATIONS, Kind.ENERGY, Kind.HISTORY
    };
    private static final String[] CARDS = {
        "dashboard", "devices", "rooms", "automations", "energy", "history"
    };

    private final MainFrame main;
    private final List<NavButton> navs = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
    private final JLabel wordmark = Theme.label("DOMOTIQ", 18, Font.BOLD, Theme.SIDEBAR_TEXT);
    private final JLabel note;
    private final JButton themeBtn;
    private final JButton collapseBtn;
    private final javax.swing.Timer ticker;

    private int widthNow = WIDTH_EXPANDED;
    private int widthFrom = WIDTH_EXPANDED;
    private int widthTo = WIDTH_EXPANDED;
    private float progress = 1f;
    private boolean collapsed;

    Sidebar(MainFrame main) {
        this.main = main;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.SIDEBAR);
        setBorder(BorderFactory.createEmptyBorder(20, SIDE_INSET, 18, SIDE_INSET));
        setPreferredSize(new Dimension(widthNow, 0));

        logoRow.setOpaque(false);
        logoRow.add(new LoginFrame.LogoMark(28));
        logoRow.add(wordmark);
        addRow(logoRow, 40);
        add(Box.createVerticalStrut(22));

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < CARDS.length; i++) {
            String card = CARDS[i];
            NavButton nav = new NavButton(LABELS[i], GLYPHS[i], "Alt+" + (i + 1));
            group.add(nav);
            nav.addActionListener(e -> main.showCard(card));
            navs.add(nav);
            addRow(nav, NAV_HEIGHT);
            if (i < CARDS.length - 1) {
                add(Box.createVerticalStrut(6));
            }
        }

        add(Box.createVerticalStrut(20));
        note = Theme.label("<html>Simulated IoT devices -<br>no physical hardware.</html>",
                11, Font.ITALIC, Theme.SIDEBAR_NOTE);
        addRow(note, 34);
        add(Box.createVerticalGlue());

        themeBtn = Theme.sidebarButton(themeLabel(), Theme.SIDEBAR_TEXT,
                glyph(themeGlyph(), Theme.SIDEBAR_TEXT, 18));
        themeBtn.addActionListener(e -> main.toggleTheme());
        addRow(themeBtn, 36);
        add(Box.createVerticalStrut(8));

        collapseBtn = Theme.sidebarButton("Collapse", Theme.SIDEBAR_TEXT,
                glyph(Kind.CHEVRON_LEFT, Theme.SIDEBAR_TEXT, 18));
        collapseBtn.setToolTipText("Collapse sidebar  (Ctrl+B)");
        collapseBtn.addActionListener(e -> toggleCollapsed());
        addRow(collapseBtn, 36);

        ticker = new javax.swing.Timer(30, e -> tick());
        applyWidth(widthNow);
    }

    /** Number of rail entries; the caller's Alt+number bindings stop here. */
    static int screenCount() {
        return CARDS.length;
    }

    /** Selects a rail entry programmatically, e.g. from the Alt+number bindings. */
    void selectIndex(int index) {
        navs.get(index).setSelected(true);
        main.showCard(CARDS[index]);
    }

    void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    private void setCollapsed(boolean value) {
        collapsed = value;
        for (NavButton nav : navs) {
            nav.setCollapsed(value);
        }
        wordmark.setVisible(!value);
        note.setVisible(!value);
        logoRow.setLayout(new FlowLayout(value ? FlowLayout.CENTER : FlowLayout.LEFT,
                value ? 0 : 9, 0));
        themeBtn.setText(value ? "" : themeLabel());
        collapseBtn.setText(value ? "" : "Expand");
        collapseBtn.setIcon(glyph(value ? Kind.CHEVRON_RIGHT : Kind.CHEVRON_LEFT,
                Theme.SIDEBAR_TEXT, 18));
        collapseBtn.setToolTipText((value ? "Expand" : "Collapse") + " sidebar  (Ctrl+B)");
        collapseBtn.getAccessibleContext().setAccessibleName(
                (value ? "Expand" : "Collapse") + " sidebar");
        animateTo(value ? WIDTH_COLLAPSED : WIDTH_EXPANDED);
    }

    private String themeLabel() {
        return Theme.mode == Theme.Mode.DARK ? "Light mode" : "Dark mode";
    }

    private Kind themeGlyph() {
        return Theme.mode == Theme.Mode.DARK ? Kind.SUN : Kind.MOON;
    }

    private void addRow(JComponent c, int height) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        rows.add(new Row(c, height));
        add(c);
    }

    private void animateTo(int target) {
        widthFrom = widthNow;
        widthTo = target;
        progress = 0f;
        ticker.restart();
    }

    private void tick() {
        progress = Math.min(1f, progress + 30f / Theme.MOTION_MS);
        float eased = 1f - (1f - progress) * (1f - progress);
        applyWidth(Math.round(widthFrom + (widthTo - widthFrom) * eased));
        if (progress >= 1f) {
            ticker.stop();
        }
    }

    /**
     * BoxLayout sizes each row from its own preferred width, so every child has to be
     * re-armed whenever the rail width changes; otherwise the widest row holds the
     * rail open at its expanded width.
     */
    private void applyWidth(int width) {
        widthNow = width;
        setPreferredSize(new Dimension(width, 0));
        setMinimumSize(new Dimension(width, 0));
        int inner = width - 2 * SIDE_INSET;
        for (Row row : rows) {
            row.comp().setPreferredSize(new Dimension(inner, row.height()));
            row.comp().setMaximumSize(new Dimension(inner, row.height()));
        }
        revalidate();
        repaint();
    }

    private static Icon glyph(Kind kind, Color color, int size) {
        return new Icon() {
            @Override public int getIconWidth() {
                return size;
            }

            @Override public int getIconHeight() {
                return size;
            }

            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                NavIcons.paint((Graphics2D) g, kind, x, y, size, color);
            }
        };
    }

    private record Row(JComponent comp, int height) {
    }
}
