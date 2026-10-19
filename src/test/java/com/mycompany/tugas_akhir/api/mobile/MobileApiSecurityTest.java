package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileApiAuthenticationController.class)
@Import({MobileApiSecurityConfig.class, MobileApiOpaqueTokenIntrospector.class, MobileApiExceptionHandler.class})
class MobileApiSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MobileApiSessionService sessionService;

    @Test
    void protectedMobileEndpointRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(delete("/api/mobile/v1/auth/sessions/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void loginEndpointIsPublicAndReturnsOpaqueSessionToken() throws Exception {
        MobileApiSessionService.IssuedSession issued = new MobileApiSessionService.IssuedSession(
                "A".repeat(43), LocalDateTime.now().plusHours(8),
                new MobileApiSessionService.MobileUser(7, "Operator", "user"));
        when(sessionService.login(eq("operator@example.com"), eq("password"), anyString())).thenReturn(issued);

        mockMvc.perform(post("/api/mobile/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileApiAuthenticationController.LoginRequest(
                                        "operator@example.com", "password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("A".repeat(43)))
                .andExpect(jsonPath("$.user.id").value(7))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void bearerTokenAuthenticatesAndAllowsSessionRevocation() throws Exception {
        String token = "B".repeat(43);
        when(sessionService.findActiveSession(token)).thenReturn(Optional.of(
                new MobileApiSessionService.SessionIdentity(1L, 7, "Operator", "user")));

        mockMvc.perform(delete("/api/mobile/v1/auth/sessions/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(sessionService).revoke(token);
    }
}
