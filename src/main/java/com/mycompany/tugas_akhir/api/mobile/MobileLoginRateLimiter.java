package com.mycompany.tugas_akhir.api.mobile;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MobileLoginRateLimiter {
    private static final int ACCOUNT_LIMIT = 10;
    private static final int ADDRESS_LIMIT = 60;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public MobileLoginRateLimiter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean allow(String remoteAddress, String email) {
        if (remoteAddress == null || remoteAddress.isBlank()) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(15);
        boolean accountAllowed = increment(digest("account\0" + remoteAddress + "\0"
                + email.trim().toLowerCase(Locale.ROOT)), now, windowStart, ACCOUNT_LIMIT);
        boolean addressAllowed = increment(digest("address\0" + remoteAddress), now, windowStart, ADDRESS_LIMIT);
        if (cleanupCounter.incrementAndGet() % 64 == 0) {
            jdbcTemplate.update("DELETE FROM mobile_api_login_attempt WHERE window_started_at < ?",
                    now.minusDays(1));
        }
        return accountAllowed && addressAllowed;
    }

    private boolean increment(byte[] key, LocalDateTime now, LocalDateTime windowStart, int limit) {
        jdbcTemplate.update("INSERT INTO mobile_api_login_attempt (attempt_key, window_started_at, attempts) "
                        + "VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE "
                        + "attempts = CASE WHEN window_started_at < ? THEN 1 ELSE attempts + 1 END, "
                        + "window_started_at = CASE WHEN window_started_at < ? THEN ? ELSE window_started_at END",
                key, now, windowStart, windowStart, now);
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT attempts FROM mobile_api_login_attempt WHERE attempt_key = ?", Integer.class, key);
        return attempts != null && attempts <= limit;
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 tidak tersedia.", exception);
        }
    }
}
