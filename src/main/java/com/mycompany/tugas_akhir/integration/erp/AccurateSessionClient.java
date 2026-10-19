package com.mycompany.tugas_akhir.integration.erp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class AccurateSessionClient {
    private final RestClient restClient;
    private final AccurateApiProperties properties;
    private final Clock clock;

    @Autowired
    public AccurateSessionClient(RestClient.Builder builder, AccurateApiProperties properties) {
        this(builder, properties, Clock.systemUTC());
    }

    AccurateSessionClient(RestClient.Builder builder, AccurateApiProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "Accurate API properties are required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        URI accountBaseUrl = properties.getAccountBaseUrl();
        if (!isAccountApiUrl(accountBaseUrl)) {
            throw new IllegalArgumentException("Accurate account API URL must use HTTPS at account.accurate.id.");
        }
        this.restClient = builder.baseUrl(accountBaseUrl.toString()).build();
    }

    public List<DatabaseInfo> listDatabases() {
        requireOAuthMode();
        JsonNode response = request("/api/db-list.do");
        JsonNode databases = response.path("d");
        if (!databases.isArray()) throw new IllegalStateException("Accurate returned an invalid database list.");
        List<DatabaseInfo> result = new ArrayList<>(databases.size());
        for (JsonNode database : databases) {
            long id = database.path("id").asLong(0);
            String alias = database.path("alias").asText("");
            if (id <= 0 || alias.isBlank()) {
                throw new IllegalStateException("Accurate returned an invalid database entry.");
            }
            result.add(new DatabaseInfo(id, alias));
        }
        return List.copyOf(result);
    }

    public DatabaseSession openDatabase(long databaseId) {
        requireOAuthMode();
        if (databaseId <= 0) throw new IllegalArgumentException("Accurate database ID must be positive.");
        JsonNode response = request("/api/open-db.do?id=" + databaseId);
        String session = response.path("session").asText("");
        String host = response.path("host").asText("");
        URI databaseHost = validDatabaseHost(host);
        if (session.isBlank() || session.length() > 512 || databaseHost == null) {
            throw new IllegalStateException("Accurate returned an invalid database session.");
        }
        return new DatabaseSession(databaseHost, session);
    }

    public DatabaseSession openApiTokenDatabase() {
        if (!properties.usesApiTokenAuth()) {
            throw new IllegalStateException("API Token database resolution requires API_TOKEN auth mode.");
        }
        JsonNode response = request("/api/api-token.do");
        URI databaseHost = validDatabaseHost(response.path("d").path("database").path("host").asText(""));
        if (databaseHost == null) {
            throw new IllegalStateException("Accurate returned an invalid API Token database host.");
        }
        return new DatabaseSession(databaseHost, null);
    }

    private JsonNode request(String path) {
        String token = properties.getApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("ACCURATE_API_TOKEN is not configured.");
        }
        if (properties.usesApiTokenAuth()
                && (properties.getApiSecret() == null || properties.getApiSecret().isBlank())) {
            throw new IllegalStateException("ACCURATE_API_SECRET is required in API_TOKEN auth mode.");
        }
        JsonNode response = restClient.get().uri(path)
                .headers(headers -> {
                    if (properties.usesApiTokenAuth()) {
                        AccurateApiTokenSigner.apply(headers, token, properties.getApiSecret(), clock);
                    } else {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve().body(JsonNode.class);
        if (response == null || !response.path("s").asBoolean(false)) {
            throw new IllegalStateException("Accurate rejected the database request.");
        }
        return response;
    }

    private void requireOAuthMode() {
        if (properties.usesApiTokenAuth()) {
            throw new IllegalStateException("API_TOKEN auth is bound to one database; use openApiTokenDatabase().");
        }
    }

    private URI validDatabaseHost(String value) {
        try {
            return validDatabaseHost(URI.create(value));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static URI validDatabaseHost(URI host) {
        String hostname = host.getHost();
        if (!"https".equalsIgnoreCase(host.getScheme()) || hostname == null
                || host.getUserInfo() != null || host.getPort() != -1
                || host.getQuery() != null || host.getFragment() != null
                || !(hostname.equalsIgnoreCase("accurate.id")
                || hostname.toLowerCase(Locale.ROOT).endsWith(".accurate.id"))
                || host.getPath() != null && !host.getPath().isEmpty()
                && !host.getPath().equals("/") && !host.getPath().equals("/accurate")) {
            return null;
        }
        return URI.create("https://" + hostname.toLowerCase(Locale.ROOT));
    }

    private static boolean isAccountApiUrl(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme())
                && "account.accurate.id".equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null && uri.getPort() == -1
                && (uri.getPath() == null || uri.getPath().isEmpty() || uri.getPath().equals("/"))
                && uri.getQuery() == null && uri.getFragment() == null;
    }

    public record DatabaseInfo(long id, String alias) {
        public DatabaseInfo {
            if (id <= 0) throw new IllegalArgumentException("Database ID must be positive.");
            if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Database alias is required.");
        }
    }

    public record DatabaseSession(URI host, String session) {
        public DatabaseSession {
            host = validDatabaseHost(Objects.requireNonNull(host, "Database host is required."));
            if (host == null) throw new IllegalArgumentException("Database host must be HTTPS on an Accurate domain.");
            if (session != null && (session.isBlank() || session.length() > 512)) {
                throw new IllegalArgumentException("Database session must be blank or at most 512 characters.");
            }
        }
    }
}
