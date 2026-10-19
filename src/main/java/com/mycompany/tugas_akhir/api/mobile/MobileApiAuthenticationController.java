package com.mycompany.tugas_akhir.api.mobile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/mobile/v1/auth/sessions")
public class MobileApiAuthenticationController {
    private final MobileApiSessionService sessionService;

    public MobileApiAuthenticationController(MobileApiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> login(@RequestBody LoginRequest request,
                                                  HttpServletRequest servletRequest) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.email().length() > 150 || request.password() == null
                || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new MobileApiExceptions.InvalidCredentials();
        }
        MobileApiSessionService.IssuedSession issued = sessionService.login(
                request.email(), request.password(), servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(new SessionResponse(
                issued.accessToken(), "Bearer", issued.expiresAt(), issued.user()));
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MobileApiExceptions.InvalidCredentials();
        }
        sessionService.revoke(authorization.substring("Bearer ".length()));
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(String email, String password) {}
    public record SessionResponse(String accessToken, String tokenType, LocalDateTime expiresAt,
                                  MobileApiSessionService.MobileUser user) {}
}
