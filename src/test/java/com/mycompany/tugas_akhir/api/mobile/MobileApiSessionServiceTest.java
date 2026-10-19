package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileApiSessionServiceTest {
    @Mock private UserService userService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MobileLoginRateLimiter rateLimiter;

    @Test
    void loginIssuesOpaqueBearerTokenAndPersistsOnlyItsHash() {
        User user = User.builder().id(7).fullName("Operator").role("user").build();
        when(rateLimiter.allow("127.0.0.1", "operator@example.com")).thenReturn(true);
        when(userService.login("operator@example.com", "not-a-real-password" )).thenReturn(user);
        MobileApiSessionService service = new MobileApiSessionService(userService, jdbcTemplate, rateLimiter);

        MobileApiSessionService.IssuedSession session = service.login(
                "operator@example.com", "not-a-real-password", "127.0.0.1");

        assertEquals(43, session.accessToken().length());
        assertTrue(session.accessToken().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(7, session.user().id());
        assertTrue(session.expiresAt().isAfter(java.time.LocalDateTime.now().plusHours(7)));
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(startsWith("INSERT INTO mobile_api_session"), arguments.capture());
        Object[] stored = arguments.getValue();
        assertInstanceOf(byte[].class, stored[0]);
        assertEquals(32, ((byte[]) stored[0]).length);
        assertFalse(Arrays.equals(session.accessToken().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                (byte[]) stored[0]));
    }

    @Test
    void loginRateLimitRunsBeforePasswordVerification() {
        when(rateLimiter.allow("10.0.0.2", "operator@example.com")).thenReturn(false);
        MobileApiSessionService service = new MobileApiSessionService(userService, jdbcTemplate, rateLimiter);

        assertThrows(MobileApiExceptions.RateLimited.class,
                () -> service.login("operator@example.com", "password", "10.0.0.2"));
        verifyNoInteractions(userService);
        verify(jdbcTemplate, never()).update(startsWith("INSERT INTO mobile_api_session"), any(Object[].class));
    }

    @Test
    void invalidTokenShapeNeverQueriesSessionStore() {
        MobileApiSessionService service = new MobileApiSessionService(userService, jdbcTemplate, rateLimiter);

        assertTrue(service.findActiveSession("bad-token").isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }
}
