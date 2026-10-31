package com.domotiq.controller;

import com.domotiq.model.User;
import com.domotiq.repository.UserRepository;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class AuthController {

    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9_]{3,50}");
    private static final Pattern EMAIL =
            Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}");

    private final UserRepository userRepository = new UserRepository();
    private User currentUser;

    public void register(String username, String fullName, String email,
                         String password, String confirmPassword) throws SQLException {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 characters: letters, digits or underscore.");
        }
        if (fullName == null || fullName.trim().length() < 2) {
            throw new IllegalArgumentException("Please enter your full name.");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userRepository.usernameExists(username)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        if (userRepository.emailExists(email.trim())) {
            throw new IllegalArgumentException("That email is already registered.");
        }
        userRepository.register(username, fullName.trim(), email.trim(), password);
    }

    /** @return the signed-in user, or null for invalid credentials. */
    public User login(String username, String password) throws SQLException {
        currentUser = userRepository.authenticate(username, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
