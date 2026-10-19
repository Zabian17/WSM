package com.mycompany.tugas_akhir.api.mobile;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MobileApiOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private final MobileApiSessionService sessionService;

    public MobileApiOpaqueTokenIntrospector(MobileApiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) throws OAuth2IntrospectionException {
        MobileApiSessionService.SessionIdentity session = sessionService.findActiveSession(token)
                .orElseThrow(() -> new BadOpaqueTokenException("Invalid bearer token"));
        String role = session.role() == null ? "user" : session.role().toLowerCase(Locale.ROOT);
        Map<String, Object> attributes = Map.of(
                "sub", session.userId().toString(),
                "userId", session.userId(),
                "sessionId", session.sessionId(),
                "fullName", session.fullName(),
                "role", role);
        return new DefaultOAuth2AuthenticatedPrincipal(session.userId().toString(), attributes,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))));
    }
}
