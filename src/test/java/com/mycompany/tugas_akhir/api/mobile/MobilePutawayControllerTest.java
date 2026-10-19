package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.InboundReceiptStatus;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.service.InboundReceiptService;
import com.mycompany.tugas_akhir.service.InboundReceiptService.PutawayRecommendation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobilePutawayController.class)
@Import({MobileApiSecurityConfig.class, MobileApiExceptionHandler.class})
class MobilePutawayControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MobileApiOpaqueTokenIntrospector tokenIntrospector;
    @MockitoBean private InboundReceiptService receiptService;

    @Test
    void listsPendingPutawayTasksWithOnlyMobileFields() throws Exception {
        authenticateToken("mobile-token");
        InboundReceipt receipt = org.mockito.Mockito.mock(InboundReceipt.class);
        var item = org.mockito.Mockito.mock(com.mycompany.tugas_akhir.model.Barang.class);
        when(receipt.getId()).thenReturn(6);
        when(receipt.getReceiptCode()).thenReturn("RCV-6");
        when(receipt.getBarang()).thenReturn(item);
        when(item.getKodeBarang()).thenReturn("SKU-6");
        when(item.getNamaBarang()).thenReturn("Widget");
        when(receipt.getQuantity()).thenReturn(12);
        when(receipt.getBatchCode()).thenReturn("LOT-6");
        when(receipt.getExpiryDate()).thenReturn(LocalDate.of(2027, 5, 1));
        when(receipt.getStatus()).thenReturn(InboundReceiptStatus.PUTAWAY_PENDING);
        when(receipt.getCreatedAt()).thenReturn(LocalDateTime.parse("2026-10-19T10:00:00"));
        when(receiptService.getMobilePendingPutaway(PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(receipt), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/mobile/v1/putaway/receipts")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receiptCode").value("RCV-6"))
                .andExpect(jsonPath("$.data[0].itemCode").value("SKU-6"))
                .andExpect(jsonPath("$.data[0].quantity").value(12))
                .andExpect(jsonPath("$.data[0].notes").doesNotExist())
                .andExpect(jsonPath("$.pagination.totalItems").value(1));
    }

    @Test
    void exposesRecommendationsAndCompletesUsingBothScans() throws Exception {
        authenticateToken("mobile-token");
        InboundReceipt receipt = org.mockito.Mockito.mock(InboundReceipt.class);
        when(receipt.getStatus()).thenReturn(InboundReceiptStatus.PUTAWAY_PENDING);
        when(receiptService.findReceipt(6)).thenReturn(Optional.of(receipt));
        Rak rack = new Rak();
        rack.setId(9);
        rack.setKodeRak("R-09");
        when(receiptService.recommendPutawayLocations(6)).thenReturn(List.of(
                new PutawayRecommendation(rack, 20, true, true, BigDecimal.TEN)));
        when(receiptService.putawayWithScannedBarcodes(6,
                "WMS1|U0tVLTE|TE9ULTY|2027-05-01", "SLOC1|Ui0wOQ", 9, 7)).thenReturn(true);

        mockMvc.perform(get("/api/mobile/v1/putaway/receipts/6/suggestions")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rackCode").value("R-09"));
        mockMvc.perform(post("/api/mobile/v1/putaway/receipts/6/complete")
                        .header("Authorization", "Bearer mobile-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "itemBarcode", "WMS1|U0tVLTE|TE9ULTY|2027-05-01",
                                "locationBarcode", "SLOC1|Ui0wOQ", "rackId", 9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUTAWAY"));
        verify(receiptService).putawayWithScannedBarcodes(6,
                "WMS1|U0tVLTE|TE9ULTY|2027-05-01", "SLOC1|Ui0wOQ", 9, 7);
    }

    @Test
    void rejectsPutawayWhenScansDoNotMatch() throws Exception {
        authenticateToken("mobile-token");
        InboundReceipt receipt = org.mockito.Mockito.mock(InboundReceipt.class);
        when(receipt.getStatus()).thenReturn(InboundReceiptStatus.PUTAWAY_PENDING);
        when(receiptService.findReceipt(6)).thenReturn(Optional.of(receipt));
        when(receiptService.putawayWithScannedBarcodes(6, "bad", "bad", 9, 7)).thenReturn(false);

        mockMvc.perform(post("/api/mobile/v1/putaway/receipts/6/complete")
                        .header("Authorization", "Bearer mobile-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "itemBarcode", "bad", "locationBarcode", "bad", "rackId", 9))))
                .andExpect(status().isBadRequest());
    }

    private void authenticateToken(String token) {
        when(tokenIntrospector.introspect(token)).thenReturn(new DefaultOAuth2AuthenticatedPrincipal(
                "7", Map.of("userId", 7), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
