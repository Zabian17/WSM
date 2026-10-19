package com.mycompany.tugas_akhir.integration.erp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class AccurateSessionClient {
    private final RestClient restClient;
    private final AccurateApiProperties properties;

    public AccurateSessionClient(RestClient.Builder builder, AccurateApiProperties properties) {
        this.properties = Objects.requireNonNull(properties, "Accurate API properties are required.");
        URI accountBaseUrl = properties.getAccountBaseUrl();
        if (!isAccountApiUrl(accountBaseUrl)) {
            throw new IllegalArgumentException("Accurate account API URL must use HTTPS at account.accurate.id.");
        }
        this.restClient = builder.baseUrl(accountBaseUrl.toString()).build();
    }

    public List<DatabaseInfo> listDatabases() {
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

    private JsonNode request(String path) {
        String token = properties.getApiToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("ACCURATE_API_TOKEN is not configured.");
        }
        JsonNode response = restClient.get().uri(path)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(JsonNode.class);
        if (response == null || !response.path("s").asBoolean(false)) {
            throw new IllegalStateException("Accurate rejected the database request.");
        }
        return response;
    }

    private URI validDatabaseHost(String value) {
        try {
            URI host = URI.create(value);
            String hostname = host.getHost();
            if (!"https".equalsIgnoreCase(host.getScheme()) || hostname == null
                    || host.getUserInfo() != null || host.getPort() != -1
                    || host.getQuery() != null || host.getFragment() != null
                    || !(hostname.equalsIgnoreCase("accurate.id")
                    || hostname.toLowerCase(Locale.ROOT).endsWith(".accurate.id"))
                    || host.getPath() != null && !host.getPath().isEmpty() && !host.getPath().equals("/")) {
                return null;
            }
            return URI.create("https://" + hostname.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
            Objects.requireNonNull(host, "Database host is required.");
            if (session == null || session.isBlank()) throw new IllegalArgumentException("Database session is required.");
        }
    }
}
