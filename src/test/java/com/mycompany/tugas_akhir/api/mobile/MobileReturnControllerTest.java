package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.ReturnRequest;
import com.mycompany.tugas_akhir.model.ReturnRequestLine;
import com.mycompany.tugas_akhir.model.ReturnStatus;
import com.mycompany.tugas_akhir.service.ReturnRequestService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileReturnController.class)
@Import({MobileApiSecurityConfig.class, MobileApiOpaqueTokenIntrospector.class, MobileApiExceptionHandler.class})
class MobileReturnControllerTest {
    private static final String TOKEN = "R".repeat(43);
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ReturnRequestService returnRequestService;
    @MockitoBean private MobileApiSessionService sessionService;

    @BeforeEach
    void authenticateWarehouseUser() {
        when(sessionService.findActiveSession(TOKEN)).thenReturn(Optional.of(
                new MobileApiSessionService.SessionIdentity(9L, 7, "Warehouse", "user")));
    }

    @Test
    void listsApprovedReturns() throws Exception {
        when(returnRequestService.getMobileApprovedReturns(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

        mockMvc.perform(get("/api/mobile/v1/returns").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.pagination.page").value(0));
    }

    @Test
    void postsOnlyAnApprovedReturnAndPassesAuthenticatedUserAndScans() throws Exception {
        ReturnRequest request = mock(ReturnRequest.class);
        ReturnRequestLine line = mock(ReturnRequestLine.class);
        when(request.getStatus()).thenReturn(ReturnStatus.APPROVED);
        when(request.getLines()).thenReturn(List.of(line));
        when(returnRequestService.getMobileApprovedReturn(15)).thenReturn(Optional.of(request));
        when(returnRequestService.postApprovedReturnWithBarcodes(eq(15), eq(7), any())).thenReturn(true);

        mockMvc.perform(post("/api/mobile/v1/returns/15/post").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new MobileReturnController.ReturnScanSubmission(
                                List.of(new MobileReturnController.ReturnScan(1,
                                        "WMS1|U0tVLTE|UkVUVVJOLUxPVA|2030-01-01", "SLOC1|QS0wMQ"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
        verify(returnRequestService).postApprovedReturnWithBarcodes(eq(15), eq(7), any());
    }

    @Test
    void hidesReturnsThatAreNotApproved() throws Exception {
        when(returnRequestService.getMobileApprovedReturn(15)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mobile/v1/returns/15").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }
}
