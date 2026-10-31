package com.domotiq.util;

import javax.crypto.SecretKeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/** PBKDF2-HMAC-SHA256 password hashing with a per-user random salt. */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public record HashedPassword(String hash, String salt) {
    }

    public static HashedPassword hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        String hexSalt = HexFormat.of().formatHex(salt);
        return new HashedPassword(derive(password, hexSalt), hexSalt);
    }

    /** Constant-time comparison of the candidate against the stored hash. */
    public static boolean matches(String candidate, String storedHash, String storedSaltHex) {
        String computed = derive(candidate, storedSaltHex);
        return MessageDigest.isEqual(
                computed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String derive(String password, String saltHex) {
        try {
            var spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    HexFormat.of().parseHex(saltHex),
                    ITERATIONS,
                    KEY_LENGTH_BITS);
            byte[] key = SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(key);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
