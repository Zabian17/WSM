package com.mycompany.tugas_akhir;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHelperTest {

    @Test
    void newPasswordHashesUseBcrypt() {
        String hash = PasswordHelper.hashPassword("strong-password");

        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordHelper.verifyPassword("strong-password", hash));
        assertFalse(PasswordHelper.verifyPassword("wrong-password", hash));
        assertFalse(PasswordHelper.needsUpgrade(hash));
    }

    @Test
    void plaintextIsRejectedAndLegacySha256CanBeVerifiedForMigration() throws Exception {
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        String legacyHash = Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(digest.digest("legacy-password".getBytes(StandardCharsets.UTF_8)));

        assertFalse(PasswordHelper.verifyPassword("legacy-password", "legacy-password"));
        assertTrue(PasswordHelper.isLegacyPlaintextHash("legacy-password"));
        assertTrue(PasswordHelper.needsUpgrade("legacy-password"));
        assertTrue(PasswordHelper.verifyPassword("legacy-password", legacyHash));
        assertTrue(PasswordHelper.needsUpgrade(legacyHash));
        assertFalse(PasswordHelper.verifyPassword("wrong-password", legacyHash));
    }
}
