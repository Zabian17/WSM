package com.mycompany.tugas_akhir.integration.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.PERMANENT_REDIRECT;

class AccurateDatabaseClientTest {
    private static final AccurateSessionClient.DatabaseSession SESSION =
            new AccurateSessionClient.DatabaseSession(URI.create("https://public.accurate.id"), "db-session");

    @Test
    void sendsGetQueryAndSessionHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateDatabaseClient client = new AccurateDatabaseClient(builder, properties(), new ObjectMapper());
        server.expect(requestTo("https://public.accurate.id/accurate/api/item/list.do?pageSize=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("X-Session-ID", "db-session"))
                .andRespond(withSuccess("{\"s\":true,\"d\":[]}", APPLICATION_JSON));

        assertTrue(client.get(SESSION, "item/list", Map.of("pageSize", "20")).path("s").asBoolean());
        server.verify();
    }

    @Test
    void follows308ForPostAndPreservesFormAndAuthenticationHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateDatabaseClient client = new AccurateDatabaseClient(builder, properties(), new ObjectMapper());
        String endpoint = "/accurate/api/item/save.do";
        server.expect(requestTo("https://public.accurate.id" + endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("X-Session-ID", "db-session"))
                .andExpect(content().contentType(APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("name=Test+Item"))
                .andRespond(withStatus(PERMANENT_REDIRECT)
                        .header("Location", "https://pvt1.accurate.id" + endpoint));
        server.expect(requestTo("https://pvt1.accurate.id" + endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("X-Session-ID", "db-session"))
                .andExpect(content().contentType(APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("name=Test+Item"))
                .andRespond(withSuccess("{\"s\":true}", APPLICATION_JSON));

        assertTrue(client.post(SESSION, "item/save", Map.of("name", "Test Item")).path("s").asBoolean());
        server.verify();
    }

    @Test
    void refuses308RedirectOutsideAccurateDomain() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateDatabaseClient client = new AccurateDatabaseClient(builder, properties(), new ObjectMapper());
        server.expect(requestTo("https://public.accurate.id/accurate/api/item/list.do"))
                .andRespond(withStatus(PERMANENT_REDIRECT)
                        .header("Location", "https://example.invalid/accurate/api/item/list.do"));

        assertThrows(IllegalStateException.class, () -> client.get(SESSION, "item/list", Map.of()));
        server.verify();
    }

    @Test
    void rejectsEndpointOutsideAccurateApiPath() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        AccurateDatabaseClient client = new AccurateDatabaseClient(builder, properties(), new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> client.get(SESSION, "../db-list", Map.of()));
    }

    @Test
    void rejectsDatabaseSessionWithUntrustedHost() {
        assertThrows(IllegalArgumentException.class, () -> new AccurateSessionClient.DatabaseSession(
                URI.create("https://example.invalid"), "db-session"));
    }

    @Test
    void rejectsOversizedRequestParametersBeforeSending() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        AccurateDatabaseClient client = new AccurateDatabaseClient(builder, properties(), new ObjectMapper());

        assertThrows(IllegalArgumentException.class,
                () -> client.post(SESSION, "item/save", Map.of("name", "x".repeat(70_000))));
    }

    private AccurateApiProperties properties() {
        AccurateApiProperties properties = new AccurateApiProperties();
        properties.setApiToken("test-token");
        return properties;
    }
}
