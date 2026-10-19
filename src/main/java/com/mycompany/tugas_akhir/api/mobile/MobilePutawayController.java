package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.InboundReceiptStatus;
import com.mycompany.tugas_akhir.service.InboundReceiptService;
import com.mycompany.tugas_akhir.service.InboundReceiptService.PutawayRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/v1/putaway/receipts")
public class MobilePutawayController {
    private static final int MAX_PAGE_SIZE = 100;
    private final InboundReceiptService receiptService;

    public MobilePutawayController(InboundReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public MobilePage<PutawayTask> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<InboundReceipt> result = receiptService.getMobilePendingPutaway(PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toTask).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{receiptId}/suggestions")
    public List<RackSuggestion> suggestions(@PathVariable Integer receiptId) {
        requirePendingReceipt(receiptId);
        return receiptService.recommendPutawayLocations(receiptId).stream().map(this::toSuggestion).toList();
    }

    @PostMapping("/{receiptId}/complete")
    public ResponseEntity<PutawayResult> complete(@PathVariable Integer receiptId,
                                                  @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                                  @RequestBody PutawayScan submission) {
        requirePendingReceipt(receiptId);
        if (submission == null || !isBarcode(submission.itemBarcode())
                || !isBarcode(submission.locationBarcode())
                || submission.rackId() == null || submission.rackId() < 1) {
            throw new IllegalArgumentException("Scan barang, lokasi, dan ID rak wajib diisi.");
        }
        Integer userId = userId(user);
        boolean completed = receiptService.putawayWithScannedBarcodes(receiptId,
                submission.itemBarcode(), submission.locationBarcode(), submission.rackId(), userId);
        if (!completed) throw new IllegalArgumentException("Barcode barang atau lokasi tidak cocok.");
        return ResponseEntity.ok(new PutawayResult(InboundReceiptStatus.PUTAWAY));
    }

    private InboundReceipt requirePendingReceipt(Integer receiptId) {
        Optional<InboundReceipt> receipt = receiptService.findReceipt(receiptId);
        if (receipt.isEmpty()) throw new MobileApiExceptions.NotFound();
        if (receipt.get().getStatus() != InboundReceiptStatus.PUTAWAY_PENDING) {
            throw new IllegalStateException("Receipt tidak sedang menunggu putaway.");
        }
        return receipt.get();
    }

    private Integer userId(OAuth2AuthenticatedPrincipal user) {
        Object id = user == null ? null : user.getAttribute("userId");
        if (id instanceof Integer userId) return userId;
        throw new MobileApiExceptions.InvalidCredentials();
    }

    private boolean isBarcode(String barcode) {
        return barcode != null && !barcode.isBlank() && barcode.length() <= 512;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page harus >= 0 dan size harus antara 1 dan "
                    + MAX_PAGE_SIZE + ".");
        }
    }

    private PutawayTask toTask(InboundReceipt receipt) {
        return new PutawayTask(receipt.getId(), receipt.getReceiptCode(), receipt.getBarang().getKodeBarang(),
                receipt.getBarang().getNamaBarang(), receipt.getQuantity(), receipt.getBatchCode(),
                receipt.getExpiryDate(), receipt.getStatus(), receipt.getCreatedAt());
    }

    private RackSuggestion toSuggestion(PutawayRecommendation recommendation) {
        return new RackSuggestion(recommendation.rack().getId(), recommendation.rack().getKodeRak(),
                recommendation.availableCapacity(), recommendation.categoryMatch(),
                recommendation.storageTypeMatch(), recommendation.availableCbm());
    }

    public record PutawayTask(Integer id, String receiptCode, String itemCode, String itemName,
                              Integer quantity, String batchCode, LocalDate expiryDate,
                              InboundReceiptStatus status, LocalDateTime createdAt) {}
    public record RackSuggestion(Integer rackId, String rackCode, Integer availableCapacity,
                                 boolean categoryMatch, boolean storageTypeMatch, BigDecimal availableCbm) {}
    public record PutawayScan(String itemBarcode, String locationBarcode, Integer rackId) {}
    public record PutawayResult(InboundReceiptStatus status) {}
}
