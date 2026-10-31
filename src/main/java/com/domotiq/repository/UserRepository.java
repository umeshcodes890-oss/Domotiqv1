package com.domotiq.repository;

import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.User;
import com.domotiq.util.PasswordHasher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class UserRepository {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User register(String username, String fullName, String email, String password)
            throws SQLException {
        PasswordHasher.HashedPassword hp = PasswordHasher.hash(password);
        String sql = """
                INSERT INTO users (username, full_name, email, password_hash, password_salt)
                VALUES (?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, hp.hash());
            ps.setString(5, hp.salt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getInt(1), username, fullName, email);
            }
        }
    }

    /** Returns the authenticated user, or null when credentials are wrong. */
    public User authenticate(String username, String password) throws SQLException {
        String sql = """
                SELECT user_id, username, full_name, email, password_hash, password_salt, created_at
                FROM users WHERE username = ?""";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // hash a dummy value so a missing user is not detectable by timing
                    PasswordHasher.matches(password, "0".repeat(64), "0".repeat(64));
                    return null;
                }
                if (!PasswordHasher.matches(
                        password, rs.getString("password_hash"), rs.getString("password_salt"))) {
                    return null;
                }
                User user = new User(rs.getInt("user_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("email"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    user.setCreatedAt(created.toLocalDateTime());
                }
                return user;
            }
        }
    }
}
