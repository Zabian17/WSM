package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.MobileFulfillmentSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileFulfillmentController.class)
@Import({MobileApiSecurityConfig.class, MobileApiOpaqueTokenIntrospector.class, MobileApiExceptionHandler.class})
class MobileFulfillmentControllerTest {
    private static final String TOKEN = "F".repeat(43);
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private DeliveryOrderService deliveryOrderService;
    @MockitoBean private MobileApiSessionService sessionService;

    @BeforeEach
    void authenticateWarehouseUser() {
        when(sessionService.findActiveSession(TOKEN)).thenReturn(Optional.of(
                new MobileApiSessionService.SessionIdentity(9L, 7, "Warehouse", "user")));
    }

    @Test
    void listsOrdersReadyForPackingLoadingOrDispatch() throws Exception {
        when(deliveryOrderService.getMobileFulfillmentOrders(any())).thenReturn(new PageImpl<>(List.of(
                new MobileFulfillmentSummary(7, "DO-7", "Customer", DeliveryOrderStatus.PICKED,
                        LocalDateTime.of(2026, 10, 19, 9, 0))), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/mobile/v1/fulfillment/orders").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderCode").value("DO-7"))
                .andExpect(jsonPath("$.data[0].status").value("PICKED"));
    }

    @Test
    void recordsItemScanOnlyWithDoBarcodeAndAuthenticatedActor() throws Exception {
        when(deliveryOrderService.recordMobileFulfillmentScan(7, 8, "DO-7", "WMS1|SKU", 2, 7))
                .thenReturn(true);

        mockMvc.perform(post("/api/mobile/v1/fulfillment/orders/7/scans")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new MobileFulfillmentController.FulfillmentScan(
                                8, 2, "DO-7", "WMS1|SKU"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(true));
        verify(deliveryOrderService).recordMobileFulfillmentScan(7, 8, "DO-7", "WMS1|SKU", 2, 7);
    }

    @Test
    void completesPackingAndDispatchWithRealStageStatuses() throws Exception {
        when(deliveryOrderService.completeMobileFulfillment(7, 7)).thenReturn(DeliveryOrderStatus.PACKED);
        when(deliveryOrderService.dispatchMobileFulfillment(7, 7)).thenReturn(DeliveryOrderStatus.PARTIALLY_DISPATCHED);

        mockMvc.perform(post("/api/mobile/v1/fulfillment/orders/7/complete")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"));
        mockMvc.perform(post("/api/mobile/v1/fulfillment/orders/7/dispatch")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_DISPATCHED"));
    }

    @Test
    void doesNotExposeOrdersOutsideFulfillmentStages() throws Exception {
        when(deliveryOrderService.getMobileFulfillmentOrder(7)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mobile/v1/fulfillment/orders/7")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }
}
