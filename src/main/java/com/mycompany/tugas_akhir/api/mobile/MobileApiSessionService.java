package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MobileApiSessionService {
    private static final long SESSION_HOURS = 8;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;
    private final MobileLoginRateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    public MobileApiSessionService(UserService userService, JdbcTemplate jdbcTemplate,
                                   MobileLoginRateLimiter rateLimiter) {
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public IssuedSession login(String email, String password, String remoteAddress) {
        if (email == null || email.isBlank() || email.length() > 150 || password == null
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new MobileApiExceptions.InvalidCredentials();
        }
        if (!rateLimiter.allow(remoteAddress, email)) throw new MobileApiExceptions.RateLimited();
        User user = userService.login(email.trim().toLowerCase(Locale.ROOT), password);
        if (user == null) throw new MobileApiExceptions.InvalidCredentials();

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(SESSION_HOURS);
        jdbcTemplate.update("INSERT INTO mobile_api_session "
                        + "(token_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
                digest(token), user.getId(), now, expiresAt);
        return new IssuedSession(token, expiresAt,
                new MobileUser(user.getId(), user.getFullName(), user.getRole()));
    }

    @Transactional(readOnly = true)
    public Optional<SessionIdentity> findActiveSession(String token) {
        if (token == null || token.length() != 43 || !token.matches("[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }
        List<SessionIdentity> sessions = jdbcTemplate.query(
                "SELECT s.id, u.id AS user_id, u.full_name, u.role FROM mobile_api_session s "
                        + "JOIN users u ON u.id = s.user_id WHERE s.token_hash = ? "
                        + "AND s.expires_at > ? AND s.revoked_at IS NULL",
                (result, rowNumber) -> new SessionIdentity(result.getLong("id"),
                        result.getInt("user_id"), result.getString("full_name"), result.getString("role")),
                digest(token), LocalDateTime.now());
        return sessions.stream().findFirst();
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.length() != 43 || !token.matches("[A-Za-z0-9_-]{43}")) return;
        jdbcTemplate.update("UPDATE mobile_api_session SET revoked_at = ? "
                        + "WHERE token_hash = ? AND revoked_at IS NULL",
                LocalDateTime.now(), digest(token));
    }

    private byte[] digest(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 tidak tersedia.", exception);
        }
    }

    public record MobileUser(Integer id, String fullName, String role) {}
    public record IssuedSession(String accessToken, LocalDateTime expiresAt, MobileUser user) {}
    public record SessionIdentity(long sessionId, Integer userId, String fullName, String role) {}
}
