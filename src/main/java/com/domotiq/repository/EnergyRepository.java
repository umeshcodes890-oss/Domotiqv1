package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Daily kWh estimates accumulated from simulated device uptime.
 * Estimates only - DOMOTIQ has no physical meters.
 */
public class EnergyRepository {

    public record DailyTotal(LocalDate date, double kwh) { }

    public record DeviceTotal(int deviceId, String deviceName, String deviceType, double kwh) { }

    /** Upsert: adds the delta to today's row for the device. */
    public void addKwh(int deviceId, int userId, LocalDate day, double deltaKwh)
            throws SQLException {
        String sql = """
                INSERT INTO energy_readings (device_id, user_id, reading_date, kwh)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE kwh = kwh + VALUES(kwh)""";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            ps.setInt(2, userId);
            ps.setDate(3, Date.valueOf(day));
            ps.setDouble(4, deltaKwh);
            ps.executeUpdate();
        }
    }

    public DailyTotal[] dailyTotals(int userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = """
                SELECT reading_date, SUM(kwh) AS total
                FROM energy_readings
                WHERE user_id = ? AND reading_date BETWEEN ? AND ?
                GROUP BY reading_date
                ORDER BY reading_date""";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                var list = new java.util.ArrayList<DailyTotal>();
                while (rs.next()) {
                    list.add(new DailyTotal(rs.getDate("reading_date").toLocalDate(),
                            rs.getDouble("total")));
                }
                return list.toArray(new DailyTotal[0]);
            }
        }
    }

    public DeviceTotal[] deviceTotals(int userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = """
                SELECT e.device_id, d.device_name, d.device_type, SUM(e.kwh) AS total
                FROM energy_readings e
                JOIN devices d ON d.device_id = e.device_id
                WHERE e.user_id = ? AND e.reading_date BETWEEN ? AND ?
                GROUP BY e.device_id, d.device_name, d.device_type
                ORDER BY total DESC""";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                var list = new java.util.ArrayList<DeviceTotal>();
                while (rs.next()) {
                    list.add(new DeviceTotal(rs.getInt("device_id"),
                            rs.getString("device_name"),
                            rs.getString("device_type"),
                            rs.getDouble("total")));
                }
                return list.toArray(new DeviceTotal[0]);
            }
        }
    }

    public double totalBetween(int userId, LocalDate from, LocalDate to) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "SELECT COALESCE(SUM(kwh), 0) FROM energy_readings"
                        + " WHERE user_id = ? AND reading_date BETWEEN ? AND ?")) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    /** Drops accumulated deltas for a device (used when a device is deleted mid-day). */
    public void deleteForDevice(int deviceId) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "DELETE FROM energy_readings WHERE device_id = ?")) {
            ps.setInt(1, deviceId);
            ps.executeUpdate();
        }
    }
}
