package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.StockCountStatus;
import com.mycompany.tugas_akhir.repository.StockCountLineProjection;
import com.mycompany.tugas_akhir.repository.StockCountReviewLineProjection;
import com.mycompany.tugas_akhir.service.StockCountService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileStockCountController.class)
@Import({MobileApiSecurityConfig.class, MobileApiExceptionHandler.class})
class MobileStockCountControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MobileApiOpaqueTokenIntrospector tokenIntrospector;
    @MockitoBean private StockCountService stockCountService;

    @Test
    void countLinesRequireAuthenticationAndDoNotExposeExpectedStock() throws Exception {
        authenticateToken("mobile-token");
        when(stockCountService.getCountStatus(9)).thenReturn(StockCountStatus.COUNTING);
        when(stockCountService.getMobileCountLines(eq(9), eq(7), eq(PageRequest.of(0, 100))))
                .thenReturn(new PageImpl<>(List.of(new StockCountLineProjection(21, "SKU-1", "Item", "pcs", null)),
                        PageRequest.of(0, 100), 1));

        mockMvc.perform(get("/api/mobile/v1/stock-counts/9/lines")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCode").value("SKU-1"))
                .andExpect(jsonPath("$.data[0].expectedQuantity").doesNotExist())
                .andExpect(jsonPath("$.data[0].countedQuantity").doesNotExist())
                .andExpect(jsonPath("$.pagination.totalItems").value(1));
        verify(stockCountService).getMobileCountLines(9, 7, PageRequest.of(0, 100));
    }

    @Test
    void mobileCountCanValidateScannedQrAndSubmitOneBatch() throws Exception {
        authenticateToken("mobile-token");
        when(stockCountService.getCountStatus(9)).thenReturn(StockCountStatus.COUNTING);

        mockMvc.perform(put("/api/mobile/v1/stock-counts/9/lines")
                        .header("Authorization", "Bearer mobile-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("counts", List.of(Map.of(
                                "lineId", 21, "quantity", 4,
                                "barcodePayload", "WMS1|U0tVLTE|QkFUQ0gtMQ|-"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COUNTING"));

        verify(stockCountService).validateMobileCountBarcode(9, 21, 7, "WMS1|U0tVLTE|QkFUQ0gtMQ|-");
        verify(stockCountService).submitCounts(9, Map.of(21, 4), 7);
    }

    @Test
    void completedCountReviewShowsVarianceOnlyAfterAllLinesAreCounted() throws Exception {
        authenticateToken("mobile-token");
        when(stockCountService.getCountStatus(9)).thenReturn(StockCountStatus.COUNTED);
        when(stockCountService.getMobileCountReviewLines(eq(9), eq(PageRequest.of(0, 100))))
                .thenReturn(new PageImpl<>(List.of(new StockCountReviewLineProjection(
                        21, "SKU-1", "Item", "pcs", 12, 10)), PageRequest.of(0, 100), 1));

        mockMvc.perform(get("/api/mobile/v1/stock-counts/9/review")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].expectedQuantity").value(12))
                .andExpect(jsonPath("$.data[0].countedQuantity").value(10))
                .andExpect(jsonPath("$.data[0].variance").value(-2));
    }

    @Test
    void postsCompletedCountThroughStockLedger() throws Exception {
        authenticateToken("mobile-token");
        when(stockCountService.getCountStatus(9)).thenReturn(StockCountStatus.COUNTED);

        mockMvc.perform(post("/api/mobile/v1/stock-counts/9/post")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));

        verify(stockCountService).postCount(9, 7);
    }

    @Test
    void incompleteCountCannotBeReviewedOrPosted() throws Exception {
        authenticateToken("mobile-token");
        when(stockCountService.getCountStatus(9)).thenReturn(StockCountStatus.COUNTING);

        mockMvc.perform(get("/api/mobile/v1/stock-counts/9/review")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/mobile/v1/stock-counts/9/post")
                        .header("Authorization", "Bearer mobile-token"))
                .andExpect(status().isConflict());

        verify(stockCountService, never()).getMobileCountReviewLines(eq(9), org.mockito.ArgumentMatchers.any());
        verify(stockCountService, never()).postCount(9, 7);
    }

    private void authenticateToken(String token) {
        when(tokenIntrospector.introspect(token)).thenReturn(new DefaultOAuth2AuthenticatedPrincipal(
                "7", Map.of("userId", 7), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
