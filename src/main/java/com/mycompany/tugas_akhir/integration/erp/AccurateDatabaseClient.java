package com.mycompany.tugas_akhir.integration.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.integration.erp.AccurateSessionClient.DatabaseSession;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AccurateDatabaseClient {
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_PARAMETERS = 100;
    private static final int MAX_PARAMETER_BYTES = 65_536;
    private static final int MAX_PARAMETER_VALUE_LENGTH = 8192;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final RestClient restClient;
    private final AccurateApiProperties properties;
    private final ObjectMapper objectMapper;

    public AccurateDatabaseClient(RestClient.Builder builder, AccurateApiProperties properties,
                                  ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode get(DatabaseSession session, String endpoint, Map<String, String> parameters) {
        return request(HttpMethod.GET, session, endpoint, parameters);
    }

    public JsonNode post(DatabaseSession session, String endpoint, Map<String, String> parameters) {
        return request(HttpMethod.POST, session, endpoint, parameters);
    }

    private JsonNode request(HttpMethod method, DatabaseSession session, String endpoint,
                             Map<String, String> parameters) {
        if (session == null || !validEndpoint(endpoint) || !validParameters(parameters)) {
            throw new IllegalArgumentException("Invalid Accurate database request.");
        }
        String token = properties.getApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("ACCURATE_API_TOKEN is not configured.");
        }
        URI target = buildUri(session.host(), endpoint, method == HttpMethod.GET ? parameters : Map.of());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (method == HttpMethod.POST) parameters.forEach(form::add);
        Set<URI> visited = new HashSet<>();
        for (int redirects = 0; ; redirects++) {
            if (!visited.add(target)) throw new IllegalStateException("Accurate redirect loop detected.");
            ExchangeResult result = restClient.method(method).uri(target)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set("X-Session-ID", session.session());
                    })
                    .body(method == HttpMethod.POST ? form : new LinkedMultiValueMap<>())
                    .exchange((request, response) -> new ExchangeResult(response.getStatusCode().value(),
                            response.getHeaders().getLocation(), readLimited(response.getBody())));
            if (result.status() == 308) {
                if (redirects >= MAX_REDIRECTS || result.location() == null) {
                    throw new IllegalStateException("Accurate redirect could not be followed safely.");
                }
                target = validateRedirect(target, result.location());
                continue;
            }
            if (result.status() < 200 || result.status() >= 300) {
                throw new IllegalStateException("Accurate database request failed with HTTP " + result.status() + ".");
            }
            JsonNode body = readResponse(result.body());
            if (!body.path("s").asBoolean(false)) {
                throw new IllegalStateException("Accurate rejected the database request.");
            }
            return body;
        }
    }

    private URI buildUri(URI host, String endpoint, Map<String, String> parameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(host)
                .path("/accurate/api/").path(endpoint).path(".do");
        parameters.forEach(builder::queryParam);
        return builder.build().encode().toUri();
    }

    private URI validateRedirect(URI current, URI location) {
        URI redirected = current.resolve(location);
        DatabaseSession validated;
        try {
            URI redirectHost = URI.create(redirected.getScheme() + "://" + redirected.getRawAuthority());
            validated = new DatabaseSession(redirectHost, "redirect-validation");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Accurate redirected to an untrusted host.");
        }
        if (!current.getRawPath().equals(redirected.getRawPath())
                || !java.util.Objects.equals(current.getRawQuery(), redirected.getRawQuery())
                || redirected.getFragment() != null) {
            throw new IllegalStateException("Accurate redirect changed the API request path or parameters.");
        }
        return validated.host().resolve(redirected.getRawPath()
                + (redirected.getRawQuery() == null ? "" : "?" + redirected.getRawQuery()));
    }

    private JsonNode readResponse(byte[] body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json == null || !json.isObject()) throw new IllegalStateException("Accurate returned invalid JSON.");
            return json;
        } catch (IOException exception) {
            throw new IllegalStateException("Accurate returned invalid JSON.");
        }
    }

    private byte[] readLimited(InputStream responseBody) throws IOException {
        byte[] body = responseBody.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (body.length > MAX_RESPONSE_BYTES) {
            throw new IllegalStateException("Accurate response exceeded the allowed size.");
        }
        return body;
    }

    private boolean validEndpoint(String endpoint) {
        return endpoint != null && endpoint.length() <= 130
                && endpoint.matches("[a-z][a-z0-9_-]{0,63}/[a-z][a-z0-9_-]{0,63}");
    }

    private boolean validParameters(Map<String, String> parameters) {
        return parameters != null && parameters.size() <= MAX_PARAMETERS
                && parameters.entrySet().stream().allMatch(entry -> entry.getKey() != null
                && entry.getKey().matches("[A-Za-z0-9_.\\[\\]-]{1,128}")
                && entry.getValue() != null && entry.getValue().length() <= MAX_PARAMETER_VALUE_LENGTH)
                && parameters.entrySet().stream().mapToInt(entry -> entry.getKey().length()
                + entry.getValue().length()).sum() <= MAX_PARAMETER_BYTES;
    }

    private record ExchangeResult(int status, URI location, byte[] body) {}
}
