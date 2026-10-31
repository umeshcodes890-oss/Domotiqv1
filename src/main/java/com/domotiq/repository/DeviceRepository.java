package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.factory.DeviceFactory;
import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists devices and their key/value settings. Device power and connection
 * state are stored columns; category-specific settings live in
 * device_settings so new categories require no schema change.
 */
public class DeviceRepository {

    public List<Device> findByUser(int userId) throws SQLException {
        String sql = """
                SELECT device_id, user_id, room_id, device_name, device_type,
                       power_state, connection_status, date_added
                FROM devices WHERE user_id = ? ORDER BY device_name""";
        List<Device> devices = new ArrayList<>();
        var connection = DatabaseConnection.get();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    devices.add(map(rs));
                }
            }
        }
        for (Device d : devices) {
            d.loadSettings(readSettings(d.getId()));
        }
        return devices;
    }

    private Device map(ResultSet rs) throws SQLException {
        Device device = DeviceFactory.create(DeviceType.valueOf(rs.getString("device_type")));
        device.setId(rs.getInt("device_id"));
        device.setUserId(rs.getInt("user_id"));
        int roomId = rs.getInt("room_id");
        device.setRoomId(rs.wasNull() ? null : roomId);
        device.setName(rs.getString("device_name"));
        device.setOnInternal(rs.getBoolean("power_state"));
        device.setOnline("ONLINE".equals(rs.getString("connection_status")));
        Timestamp added = rs.getTimestamp("date_added");
        if (added != null) {
            device.setDateAdded(added.toLocalDateTime());
        }
        return device;
    }

    private Map<String, String> readSettings(int deviceId) throws SQLException {
        String sql = "SELECT setting_key, setting_value FROM device_settings WHERE device_id = ?";
        Map<String, String> map = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }
            }
        }
        return map;
    }

    public Device insert(Device device) throws SQLException {
        var connection = DatabaseConnection.get();
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String sql = """
                    INSERT INTO devices (user_id, room_id, device_name, device_type,
                                         power_state, connection_status)
                    VALUES (?, ?, ?, ?, ?, ?)""";
            try (PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, device.getUserId());
                if (device.getRoomId() == null) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(2, device.getRoomId());
                }
                ps.setString(3, device.getName());
                ps.setString(4, device.getDeviceType().name());
                ps.setBoolean(5, device.isOn());
                ps.setString(6, device.isOnline() ? "ONLINE" : "OFFLINE");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    device.setId(keys.getInt(1));
                }
            }
            writeSettings(connection, device);
            connection.commit();
            return device;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    public boolean update(Device device) throws SQLException {
        var connection = DatabaseConnection.get();
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String sql = """
                    UPDATE devices SET room_id = ?, device_name = ?, device_type = ?,
                           power_state = ?, connection_status = ?
                    WHERE device_id = ? AND user_id = ?""";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                if (device.getRoomId() == null) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, device.getRoomId());
                }
                ps.setString(2, device.getName());
                ps.setString(3, device.getDeviceType().name());
                ps.setBoolean(4, device.isOn());
                ps.setString(5, device.isOnline() ? "ONLINE" : "OFFLINE");
                ps.setInt(6, device.getId());
                ps.setInt(7, device.getUserId());
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Device not found: " + device.getId());
                }
            }
            deleteSettings(connection, device.getId());
            writeSettings(connection, device);
            connection.commit();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    public boolean updatePowerState(Device device) throws SQLException {
        String sql = "UPDATE devices SET power_state = ? WHERE device_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setBoolean(1, device.isOn());
            ps.setInt(2, device.getId());
            ps.setInt(3, device.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateConnectionStatus(Device device) throws SQLException {
        String sql = "UPDATE devices SET connection_status = ? WHERE device_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, device.isOnline() ? "ONLINE" : "OFFLINE");
            ps.setInt(2, device.getId());
            ps.setInt(3, device.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateSettings(Device device) throws SQLException {
        var connection = DatabaseConnection.get();
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            deleteSettings(connection, device.getId());
            writeSettings(connection, device);
            connection.commit();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    public boolean delete(int deviceId, int userId) throws SQLException {
        String sql = "DELETE FROM devices WHERE device_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private void writeSettings(java.sql.Connection connection, Device device) throws SQLException {
        String sql = "INSERT INTO device_settings (device_id, setting_key, setting_value) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, String> e : device.getSettings().entrySet()) {
                ps.setInt(1, device.getId());
                ps.setString(2, e.getKey());
                ps.setString(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteSettings(java.sql.Connection connection, int deviceId) throws SQLException {
        try (PreparedStatement ps = connection
                .prepareStatement("DELETE FROM device_settings WHERE device_id = ?")) {
            ps.setInt(1, deviceId);
            ps.executeUpdate();
        }
    }
}
