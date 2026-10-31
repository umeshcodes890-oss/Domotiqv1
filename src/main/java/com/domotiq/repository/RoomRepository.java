package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.Room;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {

    public List<Room> findByUser(int userId) throws SQLException {
        String sql = "SELECT room_id, user_id, room_name, description FROM rooms WHERE user_id = ? ORDER BY room_name";
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new Room(rs.getInt("room_id"), rs.getInt("user_id"),
                            rs.getString("room_name"), rs.getString("description")));
                }
            }
        }
        return rooms;
    }

    public Room insert(int userId, String name, String description) throws SQLException {
        String sql = "INSERT INTO rooms (user_id, room_name, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Room(keys.getInt(1), userId, name, description);
            }
        }
    }

    public boolean update(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_name = ?, description = ? WHERE room_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, room.getName());
            ps.setString(2, room.getDescription());
            ps.setInt(3, room.getId());
            ps.setInt(4, room.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int roomId, int userId) throws SQLException {
        String sql = "DELETE FROM rooms WHERE room_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean nameTaken(int userId, String name, int excludeRoomId) throws SQLException {
        String sql = "SELECT 1 FROM rooms WHERE user_id = ? AND room_name = ? AND room_id <> ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setInt(3, excludeRoomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
