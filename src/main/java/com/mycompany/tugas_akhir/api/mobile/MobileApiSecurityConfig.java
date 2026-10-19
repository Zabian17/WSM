package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
public class MobileApiSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain mobileApiFilterChain(HttpSecurity http,
                                             MobileApiOpaqueTokenIntrospector introspector,
                                             ObjectMapper objectMapper) throws Exception {
        http.securityMatcher("/api/mobile/v1/**")
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/mobile/v1/auth/sessions").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .opaqueToken(token -> token.introspector(introspector))
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                objectMapper, response, HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED", "Autentikasi diperlukan.")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        writeError(objectMapper, response, HttpStatus.FORBIDDEN,
                                "FORBIDDEN", "Akses ditolak.")))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .frameOptions(frame -> frame.deny()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain denyOtherHttpRequests(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    private static void writeError(ObjectMapper objectMapper, jakarta.servlet.http.HttpServletResponse response,
                                   HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        objectMapper.writeValue(response.getOutputStream(),
                new MobileApiError(new MobileApiError.ErrorDetail(code, message)));
    }
}
