package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.service.StockTransferOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileStockTransferController.class)
@Import({MobileApiSecurityConfig.class, MobileApiExceptionHandler.class})
class MobileStockTransferControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MobileApiOpaqueTokenIntrospector tokenIntrospector;
    @MockitoBean private StockTransferOrderService service;

    @Test
    void listsOnlyAuthenticatedPickersOrders() throws Exception {
        authenticate();
        when(service.getMobilePickingOrders(org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.eq(org.springframework.data.domain.PageRequest.of(0, 25))))
                .thenReturn(org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(0, 25)));

        mockMvc.perform(get("/api/mobile/v1/stock-transfers/picking")
                        .header("Authorization", "Bearer picker-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.pagination.totalItems").value(0));
    }

    @Test
    void hidesAnOrderNotAssignedToCurrentPicker() throws Exception {
        authenticate();
        when(service.getMobilePickingDetail(18, 7)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mobile/v1/stock-transfers/picking/18")
                        .header("Authorization", "Bearer picker-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsScansThroughTheValidatedTransferService() throws Exception {
        authenticate();
        when(service.recordDispatchScan(18, 7, "WMS1|c2t1LTE|TE9ULTI|2027-05-01",
                "SLOC1|Ui0wMQ", 2)).thenReturn(true);

        mockMvc.perform(post("/api/mobile/v1/stock-transfers/picking/18/scans")
                        .header("Authorization", "Bearer picker-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("itemBarcode",
                                "WMS1|c2t1LTE|TE9ULTI|2027-05-01", "locationBarcode", "SLOC1|Ui0wMQ",
                                "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(true));
        verify(service).recordDispatchScan(18, 7, "WMS1|c2t1LTE|TE9ULTI|2027-05-01",
                "SLOC1|Ui0wMQ", 2);
    }

    private void authenticate() {
        when(tokenIntrospector.introspect("picker-token")).thenReturn(new DefaultOAuth2AuthenticatedPrincipal(
                "7", Map.of("userId", 7), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
