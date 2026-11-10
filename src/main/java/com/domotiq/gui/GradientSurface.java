package com.domotiq.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/** A full-bleed surface painted with the theme's card gradient. */
class GradientSurface extends JPanel {

    GradientSurface(LayoutManager layout) {
        setOpaque(false);
        setLayout(layout);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintSurface(g2, getWidth(), getHeight(), 0,
                Theme.CARD_TOP, Theme.CARD_BOTTOM);
        g2.dispose();
    }
}
