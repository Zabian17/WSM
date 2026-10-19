package com.mycompany.tugas_akhir.api.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tugas_akhir.model.DeliveryOrder;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.MobilePickInstruction;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobilePickingController.class)
@Import({MobileApiSecurityConfig.class, MobileApiExceptionHandler.class})
class MobilePickingControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MobileApiOpaqueTokenIntrospector tokenIntrospector;
    @MockitoBean private DeliveryOrderService deliveryOrderService;

    @Test
    void listsOnlyTasksAssignedToAuthenticatedPicker() throws Exception {
        authenticateToken("picker-token");
        PickTask task = task(32, "PICK-32", 14, "DO-14", 8, "ZONE-A");
        when(deliveryOrderService.getMobilePickTasks(7, PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/mobile/v1/picking/tasks")
                        .header("Authorization", "Bearer picker-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskCode").value("PICK-32"))
                .andExpect(jsonPath("$.data[0].orderCode").value("DO-14"))
                .andExpect(jsonPath("$.data[0].sectionCode").value("ZONE-A"));
        verify(deliveryOrderService).getMobilePickTasks(7, PageRequest.of(0, 25));
    }

    @Test
    void showsAssignedInstructionsAndRecordsBarcodeScan() throws Exception {
        authenticateToken("picker-token");
        PickTask task = task(32, "PICK-32", 14, "DO-14", 8, "ZONE-A");
        when(deliveryOrderService.getMobilePickTask(32, 7)).thenReturn(Optional.of(task));
        when(deliveryOrderService.getMobilePickInstructions(32, 7)).thenReturn(List.of(
                new MobilePickInstruction(91, "SKU-1", "Item", "pcs",
                        "R-01", "LOT-1", LocalDate.of(2027, 5, 1), 4, 2, 2)));
        when(deliveryOrderService.recordMobilePick(32, 91,
                "WMS1|U0tVLTE|TE9ULTY|2027-05-01", "SLOC1|Ui0wMQ", 2, 7)).thenReturn(true);
        when(deliveryOrderService.completeMobilePicking(32, 7)).thenReturn(true);

        mockMvc.perform(get("/api/mobile/v1/picking/tasks/32")
                        .header("Authorization", "Bearer picker-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.taskCode").value("PICK-32"))
                .andExpect(jsonPath("$.lines[0].rackCode").value("R-01"))
                .andExpect(jsonPath("$.lines[0].remainingQuantity").value(2));

        mockMvc.perform(post("/api/mobile/v1/picking/tasks/32/scans")
                        .header("Authorization", "Bearer picker-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("lineId", 91, "quantity", 2,
                                "itemBarcode", "WMS1|U0tVLTE|TE9ULTY|2027-05-01",
                                "locationBarcode", "SLOC1|Ui0wMQ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(true));

        mockMvc.perform(post("/api/mobile/v1/picking/tasks/32/complete")
                        .header("Authorization", "Bearer picker-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED"));
        verify(deliveryOrderService).recordMobilePick(32, 91,
                "WMS1|U0tVLTE|TE9ULTY|2027-05-01", "SLOC1|Ui0wMQ", 2, 7);
        verify(deliveryOrderService).completeMobilePicking(32, 7);
    }

    private PickTask task(Integer taskId, String taskCode, Integer orderId, String orderCode,
                          Integer sectionId, String sectionCode) {
        PickTask task = mock(PickTask.class);
        DeliveryOrder order = mock(DeliveryOrder.class);
        Section section = mock(Section.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getTaskCode()).thenReturn(taskCode);
        when(task.getStatus()).thenReturn(PickTaskStatus.IN_PROGRESS);
        when(task.getCreatedAt()).thenReturn(LocalDateTime.parse("2026-10-19T10:00:00"));
        when(task.getDeliveryOrder()).thenReturn(order);
        when(order.getId()).thenReturn(orderId);
        when(order.getOrderCode()).thenReturn(orderCode);
        when(task.getAssignedSection()).thenReturn(section);
        when(section.getId()).thenReturn(sectionId);
        when(section.getKodeSection()).thenReturn(sectionCode);
        return task;
    }

    private void authenticateToken(String token) {
        when(tokenIntrospector.introspect(token)).thenReturn(new DefaultOAuth2AuthenticatedPrincipal(
                "7", Map.of("userId", 7), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
