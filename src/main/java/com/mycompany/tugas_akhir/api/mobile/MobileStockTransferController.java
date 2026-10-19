package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.StockTransferOrder;
import com.mycompany.tugas_akhir.service.StockTransferOrderService;
import com.mycompany.tugas_akhir.service.StockTransferOrderService.MobilePickingDetail;
import com.mycompany.tugas_akhir.service.StockTransferOrderService.MobilePickingSummary;
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

@RestController
@RequestMapping("/api/mobile/v1/stock-transfers/picking")
public class MobileStockTransferController {
    private static final int MAX_PAGE_SIZE = 100;
    private final StockTransferOrderService service;

    public MobileStockTransferController(StockTransferOrderService service) {
        this.service = service;
    }

    @GetMapping
    public MobilePage<MobilePickingSummary> list(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<StockTransferOrder> result = service.getMobilePickingOrders(userId(user), PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(service::mobileSummary).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{orderId}")
    public MobilePickingDetail detail(@PathVariable Integer orderId,
                                      @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        return service.getMobilePickingDetail(orderId, userId(user))
                .orElseThrow(MobileApiExceptions.NotFound::new);
    }

    @PostMapping("/{orderId}/scans")
    public ResponseEntity<ScanResult> scan(@PathVariable Integer orderId,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                           @RequestBody ScanSubmission submission) {
        Integer pickerId = userId(user);
        if (submission == null || submission.quantity() == null || submission.quantity() < 1
                || !isBarcode(submission.itemBarcode()) || !isBarcode(submission.locationBarcode())) {
            throw new IllegalArgumentException("QR item, barcode lokasi, dan jumlah scan wajib valid.");
        }
        if (!service.recordDispatchScan(orderId, pickerId, submission.itemBarcode(),
                submission.locationBarcode(), submission.quantity())) {
            throw new IllegalArgumentException("Scan tidak cocok dengan STO, item, lot, lokasi, atau sisa jumlah.");
        }
        return ResponseEntity.ok(new ScanResult(true));
    }

    private Integer userId(OAuth2AuthenticatedPrincipal user) {
        Object id = user == null ? null : user.getAttribute("userId");
        if (id instanceof Integer userId) return userId;
        throw new MobileApiExceptions.InvalidCredentials();
    }

    private boolean isBarcode(String value) {
        return value != null && !value.isBlank() && value.length() <= 512;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page harus >= 0 dan size harus antara 1 dan "
                    + MAX_PAGE_SIZE + ".");
        }
    }

    public record ScanSubmission(String itemBarcode, String locationBarcode, Integer quantity) {}
    public record ScanResult(boolean recorded) {}
}
