package com.domotiq.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A card-gradient panel sized to its parent viewport. Add it as the first
 * child of a JScrollPane viewport (view components stack at index 1) to give
 * the scrollable area the same creamy surface as the cards.
 */
class GradientBackdrop extends JPanel {

    GradientBackdrop() {
        setOpaque(false);
        setLayout(null);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                setBounds(0, 0, e.getComponent().getWidth(), e.getComponent().getHeight());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintSurface(g2, getWidth(), getHeight(), 0,
                Theme.CARD_TOP, Theme.CARD_BOTTOM);
        g2.dispose();
    }
}
