package com.mycompany.tugas_akhir;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password hashing utility with temporary compatibility for existing credentials.
 */
public class PasswordHelper {

    private static final int MIN_PASSWORD_CODE_POINTS = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    public static boolean isValidNewPassword(String password) {
        return password != null
                && password.codePointCount(0, password.length()) >= MIN_PASSWORD_CODE_POINTS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_PASSWORD_BYTES;
    }

    /**
    * Hash password using BCrypt.
     * @param plainPassword password asli dari user
    * @return BCrypt hash untuk disimpan ke database
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password tidak boleh null.");
        }
        return BCRYPT.encode(plainPassword);
    }

    /**
    * Verify BCrypt and salted SHA-256 hashes while old salted hashes are upgraded
    * after successful login. Plaintext values are migrated at application startup.
     * 
     * @param plainPassword password yang dimasukkan user saat login
     * @param storedHash    hash/string password yang tersimpan di database
     * @return true jika password cocok
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }

        if (isBcryptHash(storedHash)) {
            return BCRYPT.matches(plainPassword, storedHash);
        }

        if (!storedHash.contains("$")) return false;

        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            byte[] actualHash = sha256(plainPassword, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean needsUpgrade(String storedHash) {
        return !isBcryptHash(storedHash);
    }

    public static boolean isLegacyPlaintextHash(String storedHash) {
        return storedHash != null && !isBcryptHash(storedHash) && !storedHash.contains("$");
    }

    private static boolean isBcryptHash(String storedHash) {
        return storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static byte[] sha256(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(password.getBytes(StandardCharsets.UTF_8));
    }
}
