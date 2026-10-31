package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.Automation;
import com.domotiq.model.AutomationAction;
import com.domotiq.model.TriggerType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Persistence for automations and their ordered actions (plain JDBC). */
public class AutomationRepository {

    public List<Automation> findByUser(int userId) throws SQLException {
        String sql = """
                SELECT automation_id, user_id, name, trigger_type, enabled,
                       sensor_device_id, sensor_op, sensor_value,
                       fire_time, days_mask, active_start, active_end,
                       cooldown_minutes, last_fired_at
                FROM automations
                WHERE user_id = ?
                ORDER BY name""";
        List<Automation> result = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Automation a = mapRow(rs);
                    a.getActions().addAll(findActions(a.getId()));
                    result.add(a);
                }
            }
        }
        return result;
    }

    private List<AutomationAction> findActions(int automationId) throws SQLException {
        String sql = """
                SELECT device_id, action_kind, power_target, setting_key,
                       setting_value, sort_order
                FROM automation_actions
                WHERE automation_id = ?
                ORDER BY sort_order""";
        List<AutomationAction> actions = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, automationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutomationAction act = new AutomationAction();
                    act.setDeviceId(rs.getInt("device_id"));
                    act.setKind(AutomationAction.Kind.valueOf(rs.getString("action_kind")));
                    act.setPowerTarget(rs.getObject("power_target") == null
                            ? null : rs.getBoolean("power_target"));
                    act.setSettingKey(rs.getString("setting_key"));
                    act.setSettingValue(rs.getString("setting_value"));
                    act.setSortOrder(rs.getInt("sort_order"));
                    actions.add(act);
                }
            }
        }
        return actions;
    }

    public int insert(Automation a) throws SQLException {
        Connection conn = DatabaseConnection.get();
        boolean auto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            int id = insertHeader(conn, a);
            insertActions(conn, id, a);
            conn.commit();
            a.setId(id);
            return id;
        } catch (SQLException | RuntimeException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(auto);
        }
    }

    public void update(Automation a) throws SQLException {
        Connection conn = DatabaseConnection.get();
        boolean auto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            updateHeader(conn, a);
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM automation_actions WHERE automation_id = ?")) {
                del.setInt(1, a.getId());
                del.executeUpdate();
            }
            insertActions(conn, a.getId(), a);
            conn.commit();
        } catch (SQLException | RuntimeException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(auto);
        }
    }

    public void delete(int automationId, int userId) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "DELETE FROM automations WHERE automation_id = ? AND user_id = ?")) {
            ps.setInt(1, automationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setEnabled(int automationId, int userId, boolean enabled) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "UPDATE automations SET enabled = ? WHERE automation_id = ? AND user_id = ?")) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setInt(2, automationId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void touchLastFired(int automationId, LocalDateTime when) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "UPDATE automations SET last_fired_at = ? WHERE automation_id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(when));
            ps.setInt(2, automationId);
            ps.executeUpdate();
        }
    }

    /** How many distinct automations fired at least once today. */
    public int countFiredToday(int userId) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "SELECT COUNT(*) FROM automations WHERE user_id = ?"
                        + " AND last_fired_at IS NOT NULL AND last_fired_at >= CURDATE()")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /* ---------- row/column mapping ---------- */

    private Automation mapRow(ResultSet rs) throws SQLException {
        Automation a = new Automation();
        a.setId(rs.getInt("automation_id"));
        a.setUserId(rs.getInt("user_id"));
        a.setName(rs.getString("name"));
        a.setTriggerType(TriggerType.valueOf(rs.getString("trigger_type")));
        a.setEnabled(rs.getBoolean("enabled"));
        a.setSensorDeviceId(rs.getObject("sensor_device_id") == null
                ? null : rs.getInt("sensor_device_id"));
        String op = rs.getString("sensor_op");
        a.setSensorOp(op == null ? null : Automation.SensorOp.valueOf(op));
        double v = rs.getDouble("sensor_value");
        a.setSensorValue(rs.wasNull() ? null : v);
        a.setFireTime(rs.getObject("fire_time", LocalTime.class));
        a.setDaysMask(rs.getString("days_mask"));
        a.setActiveStart(rs.getObject("active_start", LocalTime.class));
        a.setActiveEnd(rs.getObject("active_end", LocalTime.class));
        a.setCooldownMinutes(rs.getInt("cooldown_minutes"));
        Timestamp ts = rs.getTimestamp("last_fired_at");
        a.setLastFiredAt(ts == null ? null : ts.toLocalDateTime());
        return a;
    }

    private int insertHeader(Connection conn, Automation a) throws SQLException {
        String sql = """
                INSERT INTO automations
                    (user_id, name, trigger_type, enabled, sensor_device_id, sensor_op,
                     sensor_value, fire_time, days_mask, active_start, active_end,
                     cooldown_minutes, last_fired_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getUserId());
            bindShared(ps, a, 2);                     // params 2..12 = name..cooldown
            if (a.getLastFiredAt() == null) {
                ps.setNull(13, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(13, Timestamp.valueOf(a.getLastFiredAt()));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void updateHeader(Connection conn, Automation a) throws SQLException {
        String sql = """
                UPDATE automations SET
                    name = ?, trigger_type = ?, enabled = ?, sensor_device_id = ?,
                    sensor_op = ?, sensor_value = ?, fire_time = ?, days_mask = ?,
                    active_start = ?, active_end = ?, cooldown_minutes = ?
                WHERE automation_id = ? AND user_id = ?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindShared(ps, a, 1);                     // params 1..11 = name..cooldown
            ps.setInt(12, a.getId());
            ps.setInt(13, a.getUserId());
            ps.executeUpdate();
        }
    }

    /** Binds name..cooldown_minutes starting at the given placeholder index. */
    private void bindShared(PreparedStatement ps, Automation a, int from) throws SQLException {
        int i = from;
        ps.setString(i++, a.getName());
        ps.setString(i++, a.getTriggerType().name());
        ps.setInt(i++, a.isEnabled() ? 1 : 0);
        setNullableInt(ps, i++, a.getSensorDeviceId());
        ps.setString(i++, a.getSensorOp() == null ? null : a.getSensorOp().name());
        if (a.getSensorValue() == null) {
            ps.setNull(i++, Types.DECIMAL);
        } else {
            ps.setDouble(i++, a.getSensorValue());
        }
        setTime(ps, i++, a.getFireTime());
        ps.setString(i++, a.getDaysMask());
        setTime(ps, i++, a.getActiveStart());
        setTime(ps, i++, a.getActiveEnd());
        ps.setInt(i, a.getCooldownMinutes());
    }

    private void insertActions(Connection conn, int automationId, Automation a)
            throws SQLException {
        String sql = """
                INSERT INTO automation_actions
                    (automation_id, sort_order, device_id, action_kind,
                     power_target, setting_key, setting_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int order = 0;
            for (AutomationAction act : a.getActions()) {
                ps.setInt(1, automationId);
                ps.setInt(2, order++);
                ps.setInt(3, act.getDeviceId());
                ps.setString(4, act.getKind().name());
                if (act.getPowerTarget() == null) {
                    ps.setNull(5, Types.TINYINT);
                } else {
                    ps.setInt(5, act.getPowerTarget() ? 1 : 0);
                }
                ps.setString(6, act.getSettingKey());
                ps.setString(7, act.getSettingValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setTime(PreparedStatement ps, int index, LocalTime time) throws SQLException {
        if (time == null) {
            ps.setNull(index, Types.TIME);
        } else {
            ps.setTime(index, java.sql.Time.valueOf(time));
        }
    }
}
