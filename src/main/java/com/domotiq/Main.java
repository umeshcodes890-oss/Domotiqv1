package com.domotiq;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.gui.LoginFrame;

import javax.swing.SwingUtilities;
import java.sql.SQLException;

public final class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection.verify();
        } catch (Exception e) {
            System.err.println("Cannot connect to the DOMOTIQ MySQL database.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
