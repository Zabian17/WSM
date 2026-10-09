package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderStatus;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.PurchaseOrderRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApprovalRouter approvalRoutingService;

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(purchaseOrderRepository, barangRepository,
                supplierRepository, userRepository, approvalRoutingService);
        lenient().when(approvalRoutingService.canReview(any(), any())).thenReturn(true);
    }

    @Test
    void createsPendingApprovalPurchaseOrderWithRequestedLines() {
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User buyer = User.builder().id(3).fullName("Buyer").build();
        Barang item = Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build();
        when(supplierRepository.findById(2)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(3)).thenReturn(Optional.of(buyer));
        when(barangRepository.findById(1)).thenReturn(Optional.of(item));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder order = service.createPurchaseOrder(2, 3, Map.of(1, 12), "regular replenishment");

        assertNotNull(order);
        assertTrue(order.getOrderCode().startsWith("PO-"));
        assertEquals(PurchaseOrderStatus.PENDING_APPROVAL, order.getStatus());
        assertEquals(12, order.getLines().get(0).getOrderedQuantity());
        assertEquals(12, order.getLines().get(0).getRemainingQuantity());
    }

    @Test
    void rejectsInvalidPurchaseOrderQuantitiesBeforeSaving() {
        assertNull(service.createPurchaseOrder(2, 3, Map.of(1, 0), null));

        verifyNoInteractions(purchaseOrderRepository);
    }

    @Test
    void preventsCreatorFromApprovingTheirOwnPurchaseOrder() {
        PurchaseOrder order = purchaseOrder(10, 3);
        when(purchaseOrderRepository.findByIdWithLinesForUpdate(10)).thenReturn(java.util.Optional.of(order));
        when(userRepository.findById(3)).thenReturn(java.util.Optional.of(order.getCreatedBy()));

        assertFalse(service.approvePurchaseOrder(10, 3));
        assertEquals(PurchaseOrderStatus.PENDING_APPROVAL, order.getStatus());
        verify(purchaseOrderRepository, never()).save(order);
    }

    @Test
    void differentUserCanApprovePendingPurchaseOrder() {
        PurchaseOrder order = purchaseOrder(10, 3);
        User reviewer = User.builder().id(4).fullName("Approver").build();
        when(purchaseOrderRepository.findByIdWithLinesForUpdate(10)).thenReturn(java.util.Optional.of(order));
        when(userRepository.findById(4)).thenReturn(java.util.Optional.of(reviewer));

        assertTrue(service.approvePurchaseOrder(10, 4));
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
        assertEquals(reviewer, order.getReviewedBy());
        assertNotNull(order.getReviewedAt());
        assertNull(order.getReviewNotes());
    }

    @Test
    void requiresReasonToRejectAndStoresReviewAudit() {
        PurchaseOrder order = purchaseOrder(10, 3);
        User reviewer = User.builder().id(4).fullName("Approver").build();
        when(purchaseOrderRepository.findByIdWithLinesForUpdate(10)).thenReturn(java.util.Optional.of(order));
        when(userRepository.findById(4)).thenReturn(java.util.Optional.of(reviewer));

        assertFalse(service.rejectPurchaseOrder(10, 4, " "));
        assertTrue(service.rejectPurchaseOrder(10, 4, "Harga belum disetujui"));
        assertEquals(PurchaseOrderStatus.REJECTED, order.getStatus());
        assertEquals("Harga belum disetujui", order.getReviewNotes());
    }

    private PurchaseOrder purchaseOrder(int id, int creatorId) {
        PurchaseOrder order = new PurchaseOrder("PO-" + id, new Supplier(2, "Vendor", null, null, null),
                User.builder().id(creatorId).fullName("Buyer").build(), null, java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }
}
