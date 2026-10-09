package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.MaterialUsageRepository;
import com.mycompany.tugas_akhir.repository.MaterialUsageScanRepository;
import com.mycompany.tugas_akhir.repository.PickTaskRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialUsageServiceTest {

    @Mock private MaterialUsageRepository requestRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryLotBalanceRepository lotBalanceRepository;
    @Mock private OutboundStockPoster stockPoster;
    @Mock private MaterialUsageScanRepository scanRepository;
    @Mock private PickTaskRepository pickTaskRepository;
    @Mock private ApprovalRouter approvalRoutingService;
    private MaterialUsageService service;

    @BeforeEach
    void setUp() {
        service = new MaterialUsageService(requestRepository, barangRepository, rakRepository, userRepository,
                lotBalanceRepository, stockPoster, new InventoryPickingPolicy(lotBalanceRepository, "FEFO"),
                scanRepository, pickTaskRepository, approvalRoutingService);
        lenient().when(approvalRoutingService.canReview(any(), any())).thenReturn(true);
    }

    @Test
    void createsPendingRequestWithReasonAndLines() {
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(barangRepository.findById(1)).thenReturn(Optional.of(item()));
        when(requestRepository.save(any(MaterialUsage.class))).thenAnswer(call -> call.getArgument(0));

        MaterialUsage request = service.createRequest(3, "Pemakaian maintenance", Map.of(1, 4));

        assertNotNull(request);
        assertEquals(MaterialUsageStatus.PENDING_APPROVAL, request.getStatus());
        assertEquals("Pemakaian maintenance", request.getReason());
        assertEquals(4, request.getLines().get(0).getRequestedQuantity());
    }

    @Test
    void requiresDifferentReviewerAndRejectReason() {
        MaterialUsage request = request(5);
        when(requestRepository.findByIdWithLinesForUpdate(5)).thenReturn(Optional.of(request));
        when(userRepository.findById(3)).thenReturn(Optional.of(user(3)));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));

        assertFalse(service.reviewRequest(5, 3, true, null));
        assertFalse(service.reviewRequest(5, 4, false, " "));
        assertTrue(service.reviewRequest(5, 4, false, "Tidak ada otorisasi"));
        assertEquals(MaterialUsageStatus.REJECTED, request.getStatus());
        assertEquals(user(4).getId(), request.getReviewedBy().getId());
        assertEquals("Tidak ada otorisasi", request.getReviewNotes());
    }

    @Test
    void approvalCreatesPickTaskAndIssueRequiresAssignment() {
        MaterialUsage request = request(5);
        when(requestRepository.findByIdWithLinesForUpdate(5)).thenReturn(Optional.of(request));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));

        assertTrue(service.reviewRequest(5, 4, true, null));
        assertEquals(MaterialUsageStatus.APPROVED, request.getStatus());
        verify(pickTaskRepository).save(argThat(task -> task.getMaterialUsage() == request
                && task.getStatus() == PickTaskStatus.OPEN));
        assertFalse(service.issueApprovedRequest(5, 4));
    }

    @Test
    void issuesApprovedRequestAcrossAvailableLotsAndRecordsGoodsIssue() {
        MaterialUsage request = request(5);
        request.review(user(4), true, null, java.time.LocalDateTime.now());
        Rak rack1 = new Rak(2, null, "A-01", 50, 5);
        Rak rack2 = new Rak(3, null, "A-02", 50, 5);
        InventoryLotBalance lot1 = lot(rack1, "LOT-A", 2);
        InventoryLotBalance lot2 = lot(rack2, "LOT-B", 3);
        PickTask task = assignedTask(request, user(4));
        when(pickTaskRepository.findActiveByMaterialUsageId(5)).thenReturn(List.of(task));
        when(requestRepository.findByIdWithLinesForUpdate(5)).thenReturn(Optional.of(request));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(
                1, java.time.LocalDate.now())).thenReturn(List.of(lot1, lot2));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4)).thenReturn(true);
        when(stockPoster.postOutbound(1, 3, "LOT-B", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4)).thenReturn(true);

        assertTrue(service.issueApprovedRequest(5, 4));

        assertEquals(MaterialUsageStatus.ISSUED, request.getStatus());
        assertEquals(PickTaskStatus.COMPLETED, task.getStatus());
        assertNotNull(request.getIssuedAt());
        verify(stockPoster).postOutbound(1, 2, "LOT-A", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4);
        verify(stockPoster).postOutbound(1, 3, "LOT-B", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4);
    }

    @Test
    void scannedGoodsIssueSupportsMultipleItemsAndProgressiveQuantityScans() {
        MaterialUsage request = request(5);
        org.springframework.test.util.ReflectionTestUtils.setField(request.getLines().get(0),
                "requestedQuantity", 2);
        Barang item2 = Barang.builder().id(2).kodeBarang("SKU-2").namaBarang("Item 2").build();
        request.addLine(new MaterialUsageLine(item2, 3));
        request.review(user(4), true, null, java.time.LocalDateTime.now());
        PickTask task = assignedTask(request, user(4));
        when(pickTaskRepository.findActiveByMaterialUsageId(5)).thenReturn(List.of(task));
        Rak rack = new Rak(2, null, "A-01", 50, 5);
        InventoryLotBalance lot = lot(rack, "LOT-A", 4);
        InventoryLotBalance lot2 = new InventoryLotBalance(item2, rack, "RAK-2", "LOT-B",
                null, java.time.LocalDateTime.now(), 3);
        org.springframework.test.util.ReflectionTestUtils.setField(request.getLines().get(0), "id", 51);
        org.springframework.test.util.ReflectionTestUtils.setField(request.getLines().get(1), "id", 52);
        when(scanRepository.findByRequestId(5)).thenReturn(List.of());
        when(requestRepository.findByIdWithLinesForUpdate(5)).thenReturn(Optional.of(request));
        when(userRepository.findById(4)).thenReturn(Optional.of(user(4)));
        when(rakRepository.findByKodeRak("A-01")).thenReturn(Optional.of(rack));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(1, "RAK-2", "LOT-A"))
                .thenReturn(Optional.of(lot));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(1, LocalDate.now()))
                .thenReturn(List.of(lot));
        when(lotBalanceRepository.findAvailableFefoAcrossLocationsForUpdate(2, LocalDate.now()))
                .thenReturn(List.of(lot2));
        when(stockPoster.postOutbound(1, 2, "LOT-A", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4)).thenReturn(true);
        String itemQr = "WMS1|" + encode("SKU-1") + "|" + encode("LOT-A") + "|-";
        when(requestRepository.save(request)).thenReturn(request);

        assertTrue(service.issueApprovedRequestWithBarcodes(
                5, 4, itemQr, "SLOC1|" + encode("A-01"), 2));
        assertEquals(MaterialUsageStatus.PARTIALLY_ISSUED, request.getStatus());
        verify(stockPoster).postOutbound(1, 2, "LOT-A", 2,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4);

        MaterialUsageScan priorScan = new MaterialUsageScan(request, request.getLines().get(0),
                user(4), lot, "SKU-1", "A-01", "LOT-A", null, 2, java.time.LocalDateTime.now());
        when(scanRepository.findByRequestId(5)).thenReturn(List.of(), List.of(priorScan), List.of(priorScan));
        assertFalse(service.issueApprovedRequestWithBarcodes(
                5, 4, itemQr, "SLOC1|" + encode("A-01"), 3));
        when(lotBalanceRepository.findByBarangIdAndLocationKeyAndBatchCodeForUpdate(2, "RAK-2", "LOT-B"))
                .thenReturn(Optional.of(lot2));
        when(stockPoster.postOutbound(2, 2, "LOT-B", 3,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4)).thenReturn(true);

        assertTrue(service.issueApprovedRequestWithBarcodes(5, 4,
                "WMS1|" + encode("SKU-2") + "|" + encode("LOT-B") + "|-",
                "SLOC1|" + encode("A-01"), 3));
        assertEquals(MaterialUsageStatus.ISSUED, request.getStatus());
        assertEquals(PickTaskStatus.COMPLETED, task.getStatus());
        verify(stockPoster).postOutbound(2, 2, "LOT-B", 3,
                "Material Usage " + request.getRequestCode() + ": Maintenance", 4);
    }

    private MaterialUsage request(int id) {
        MaterialUsage request = new MaterialUsage("MTU-TEST", user(3), "Maintenance",
                java.time.LocalDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(request, "id", id);
        request.addLine(new MaterialUsageLine(item(), 4));
        return request;
    }

    private PickTask assignedTask(MaterialUsage request, User picker) {
        PickTask task = new PickTask("MTP-TEST", request, java.time.LocalDateTime.now());
        task.assign(picker, null, java.time.LocalDateTime.now());
        return task;
    }

    private User user(int id) {
        return User.builder().id(id).fullName("User " + id).build();
    }

    private Barang item() {
        return Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
    }

    private InventoryLotBalance lot(Rak rack, String batch, int quantity) {
        return new InventoryLotBalance(item(), rack, "RAK-" + rack.getId(), batch, null,
                java.time.LocalDateTime.now(), quantity);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
