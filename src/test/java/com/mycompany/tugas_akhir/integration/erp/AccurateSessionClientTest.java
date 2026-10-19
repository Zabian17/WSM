package com.mycompany.tugas_akhir.integration.erp;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccurateSessionClientTest {

    @Test
    void listsAccessibleDatabasesWithBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateSessionClient client = new AccurateSessionClient(builder, properties());
        server.expect(requestTo("https://account.accurate.id/api/db-list.do"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                        {"s":true,"d":[{"id":1156,"alias":"PT Rotaryana Prima"}]}
                        """, APPLICATION_JSON));

        List<AccurateSessionClient.DatabaseInfo> databases = client.listDatabases();

        assertEquals(List.of(new AccurateSessionClient.DatabaseInfo(1156, "PT Rotaryana Prima")), databases);
        server.verify();
    }

    @Test
    void opensDatabaseAndReturnsValidatedHostAndSession() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateSessionClient client = new AccurateSessionClient(builder, properties());
        server.expect(requestTo("https://account.accurate.id/api/open-db.do?id=1156"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(queryParam("id", "1156"))
                .andRespond(withSuccess("""
                        {"s":true,"session":"session-1","host":"https://public.accurate.id"}
                        """, APPLICATION_JSON));

        assertEquals(new AccurateSessionClient.DatabaseSession(URI.create("https://public.accurate.id"), "session-1"),
                client.openDatabase(1156));
        server.verify();
    }

    @Test
    void rejectsDatabaseSessionHostOutsideAccurateDomain() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AccurateSessionClient client = new AccurateSessionClient(builder, properties());
        server.expect(requestTo("https://account.accurate.id/api/open-db.do?id=1156"))
                .andRespond(withSuccess("""
                        {"s":true,"session":"session-1","host":"https://example.invalid"}
                        """, APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> client.openDatabase(1156));
        server.verify();
    }

    @Test
    void refusesToSendApiTokenToNonAccurateAccountHost() {
        AccurateApiProperties properties = new AccurateApiProperties();
        properties.setApiToken("test-token");
        properties.setAccountBaseUrl(URI.create("https://example.invalid"));

        assertThrows(IllegalArgumentException.class,
                () -> new AccurateSessionClient(RestClient.builder(), properties));
    }

    private AccurateApiProperties properties() {
        AccurateApiProperties properties = new AccurateApiProperties();
        properties.setApiToken("test-token");
        properties.setAccountBaseUrl(URI.create("https://account.accurate.id"));
        return properties;
    }
}
