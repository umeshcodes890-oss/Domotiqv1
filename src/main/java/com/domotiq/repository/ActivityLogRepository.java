package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.ActivityLog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogRepository {

    public ActivityLog insert(int userId, Integer deviceId, String deviceName, String action,
                              String previousStatus, String newStatus) throws SQLException {
        String sql = """
                INSERT INTO activity_logs
                    (user_id, device_id, device_name, action, previous_status, new_status)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            if (deviceId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, deviceId);
            }
            ps.setString(3, deviceName);
            ps.setString(4, action);
            ps.setString(5, previousStatus);
            ps.setString(6, newStatus);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new ActivityLog(keys.getLong(1), userId, deviceId, deviceName,
                        action, previousStatus, newStatus, java.time.LocalDateTime.now());
            }
        }
    }

    public List<ActivityLog> findByUser(int userId, int limit) throws SQLException {
        String sql = """
                SELECT log_id, user_id, device_id, device_name, action,
                       previous_status, new_status, performed_at
                FROM activity_logs
                WHERE user_id = ?
                ORDER BY performed_at DESC, log_id DESC
                LIMIT ?""";
        List<ActivityLog> logs = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("performed_at");
                    logs.add(new ActivityLog(
                            rs.getLong("log_id"),
                            rs.getInt("user_id"),
                            rs.getObject("device_id") == null
                                    ? null : rs.getInt("device_id"),
                            rs.getString("device_name"),
                            rs.getString("action"),
                            rs.getString("previous_status"),
                            rs.getString("new_status"),
                            ts == null ? null : ts.toLocalDateTime()));
                }
            }
        }
        return logs;
    }
}
