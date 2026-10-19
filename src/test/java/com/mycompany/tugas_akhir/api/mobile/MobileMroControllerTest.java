package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.MaterialUsage;
import com.mycompany.tugas_akhir.model.MaterialUsageStatus;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import com.mycompany.tugas_akhir.service.MaterialUsageService;
import com.mycompany.tugas_akhir.service.MaterialUsageService.MobilePickInstruction;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileMroController.class)
@Import({MobileApiSecurityConfig.class, MobileApiOpaqueTokenIntrospector.class, MobileApiExceptionHandler.class})
class MobileMroControllerTest {
    private static final String TOKEN = "M".repeat(43);
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MaterialUsageService materialUsageService;
    @MockitoBean private MobileApiSessionService sessionService;

    @BeforeEach
    void authenticatePicker() {
        when(sessionService.findActiveSession(TOKEN)).thenReturn(Optional.of(
                new MobileApiSessionService.SessionIdentity(9L, 7, "Picker", "user")));
    }

    @Test
    void listsOnlyTasksProvidedForAuthenticatedPicker() throws Exception {
        PickTask task = task();
        when(materialUsageService.getMobilePickTasks(eq(7), any())).thenReturn(
                new PageImpl<>(List.of(task), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/mobile/v1/mro/tasks").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestCode").value("MTU-1"))
                .andExpect(jsonPath("$.data[0].requestStatus").value("PARTIALLY_ISSUED"));
    }

    @Test
    void detailAndScanRequireAssignedTaskAndUseExistingVerifiedIssueService() throws Exception {
        PickTask task = task();
        when(materialUsageService.getMobilePickTask(4, 7)).thenReturn(Optional.of(task));
        when(materialUsageService.getMobilePickInstructions(4, 7)).thenReturn(List.of(
                new MobilePickInstruction(12, "SKU-1", "Barang", "pcs", 10, 3, 7)));
        mockMvc.perform(get("/api/mobile/v1/mro/tasks/4").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].remainingQuantity").value(7));

        when(materialUsageService.recordMobilePick(4, "WMS1|SKU-1", "SLOC1|R-1", 2, 7)).thenReturn(true);
        mockMvc.perform(post("/api/mobile/v1/mro/tasks/4/scans").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileMroController.MroScan(2, "WMS1|SKU-1", "SLOC1|R-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(true));
        verify(materialUsageService).recordMobilePick(4, "WMS1|SKU-1", "SLOC1|R-1", 2, 7);
    }

    @Test
    void taskOutsidePickersAssignmentIsNotFound() throws Exception {
        when(materialUsageService.getMobilePickTask(4, 7)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mobile/v1/mro/tasks/4").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    private PickTask task() {
        PickTask task = mock(PickTask.class);
        MaterialUsage request = mock(MaterialUsage.class);
        when(task.getId()).thenReturn(4);
        when(task.getTaskCode()).thenReturn("MTP-1");
        when(task.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 10, 19, 8, 0));
        when(task.getStatus()).thenReturn(PickTaskStatus.IN_PROGRESS);
        when(task.getMaterialUsage()).thenReturn(request);
        when(request.getId()).thenReturn(3);
        when(request.getRequestCode()).thenReturn("MTU-1");
        when(request.getReason()).thenReturn("Perawatan mesin");
        when(request.getStatus()).thenReturn(MaterialUsageStatus.PARTIALLY_ISSUED);
        return task;
    }
}
