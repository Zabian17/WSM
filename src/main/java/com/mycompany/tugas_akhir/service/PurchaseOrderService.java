package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.PurchaseOrderRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final BarangRepository barangRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ApprovalRouter approvalRoutingService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                BarangRepository barangRepository,
                                SupplierRepository supplierRepository,
                                UserRepository userRepository,
                                ApprovalRouter approvalRoutingService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.barangRepository = barangRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getRecentPurchaseOrders() {
        return purchaseOrderRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPendingApproval() {
        return purchaseOrderRepository.findPendingApproval();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderLine> getOpenLines() {
        return purchaseOrderRepository.findOpenLines();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderLine> getReceivedLines() {
        return purchaseOrderRepository.findReceivedLines();
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(Integer supplierId, Integer userId,
                                             Map<Integer, Integer> requestedQuantities, String notes) {
        if (requestedQuantities == null) return null;
        Map<Integer, PurchaseOrderLineRequest> lines = new LinkedHashMap<>();
        requestedQuantities.forEach((itemId, quantity) ->
                lines.put(itemId, new PurchaseOrderLineRequest(quantity, BigDecimal.ZERO)));
        return createPurchaseOrder(supplierId, userId, lines, notes, "IDR", null, null, null);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(Integer supplierId, Integer userId,
                                             Map<Integer, PurchaseOrderLineRequest> requestedLines, String notes,
                                             String currencyCode, String tenderReference, String paymentTerms,
                                             LocalDate expectedDeliveryDate) {
        if (supplierId == null || userId == null || requestedLines == null
                || requestedLines.isEmpty() || requestedLines.size() > 100) return null;
        String normalizedCurrency = normalizeCurrency(currencyCode);
        if (normalizedCurrency == null || tooLong(tenderReference, 80) || tooLong(paymentTerms, 120)
                || (expectedDeliveryDate != null && expectedDeliveryDate.isBefore(LocalDate.now()))) return null;
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        User creator = userRepository.findById(userId).orElse(null);
        if (supplier == null || creator == null) return null;

        PurchaseOrder order = new PurchaseOrder("PO-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                supplier, creator, notes, LocalDateTime.now());
        order.setCommercialDetails(normalizedCurrency, clean(tenderReference), clean(paymentTerms),
                expectedDeliveryDate);
        for (Map.Entry<Integer, PurchaseOrderLineRequest> request : requestedLines.entrySet()) {
            PurchaseOrderLineRequest details = request.getValue();
            if (request.getKey() == null || details == null || details.quantity() == null
                    || details.quantity() <= 0 || !validPrice(details.unitPrice())) return null;
            Barang barang = barangRepository.findById(request.getKey()).orElse(null);
            if (barang == null) return null;
            order.addLine(new PurchaseOrderLine(barang, details.quantity(), details.unitPrice()));
        }
        return purchaseOrderRepository.save(order);
    }

    private String normalizeCurrency(String currencyCode) {
        if (currencyCode == null || !currencyCode.trim().matches("[A-Za-z]{3}")) return null;
        String normalized = currencyCode.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return Currency.getInstance(normalized).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean validPrice(BigDecimal price) {
        return price != null && price.signum() >= 0 && price.precision() <= 19
                && Math.max(0, price.stripTrailingZeros().scale()) <= 4;
    }

    private boolean tooLong(String value, int limit) { return value != null && value.trim().length() > limit; }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record PurchaseOrderLineRequest(Integer quantity, BigDecimal unitPrice) {}

    @Transactional
    public boolean approvePurchaseOrder(Integer purchaseOrderId, Integer reviewerId) {
        return reviewPurchaseOrder(purchaseOrderId, reviewerId, true, null);
    }

    @Transactional
    public boolean rejectPurchaseOrder(Integer purchaseOrderId, Integer reviewerId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) return false;
        return reviewPurchaseOrder(purchaseOrderId, reviewerId, false, reason.trim());
    }

    private boolean reviewPurchaseOrder(Integer purchaseOrderId, Integer reviewerId,
                                        boolean approved, String notes) {
        if (purchaseOrderId == null || reviewerId == null) return false;
        PurchaseOrder order = purchaseOrderRepository.findByIdWithLinesForUpdate(purchaseOrderId).orElse(null);
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (order == null || reviewer == null || order.getStatus() !=
                com.mycompany.tugas_akhir.model.PurchaseOrderStatus.PENDING_APPROVAL
                || order.getCreatedBy().getId().equals(reviewerId)
                || !approvalRoutingService.canReview(order.getCreatedBy(), reviewer)) {
            return false;
        }
        order.setReviewedBy(reviewer);
        order.setReviewedAt(LocalDateTime.now());
        order.setReviewNotes(notes);
        order.setStatus(approved
                ? com.mycompany.tugas_akhir.model.PurchaseOrderStatus.APPROVED
                : com.mycompany.tugas_akhir.model.PurchaseOrderStatus.REJECTED);
        purchaseOrderRepository.save(order);
        return true;
    }
}
