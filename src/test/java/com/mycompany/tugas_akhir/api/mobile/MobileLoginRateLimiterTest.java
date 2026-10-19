package com.mycompany.tugas_akhir.api.mobile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:mobile-rate-limit;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MobileLoginRateLimiterTest {
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createRateLimitTable() {
        jdbcTemplate.execute("CREATE TABLE mobile_api_login_attempt (attempt_key BINARY(32) PRIMARY KEY, "
                + "window_started_at DATETIME NOT NULL, attempts INT NOT NULL)");
    }

    @Test
    void limitsRepeatedLoginAttemptsByAccountAndClientAddress() {
        MobileLoginRateLimiter limiter = new MobileLoginRateLimiter(jdbcTemplate);

        for (int i = 0; i < 10; i++) assertTrue(limiter.allow("127.0.0.1", "operator@example.com"));
        assertFalse(limiter.allow("127.0.0.1", "operator@example.com"));
        assertTrue(limiter.allow("127.0.0.1", "another@example.com"));
    }
}
