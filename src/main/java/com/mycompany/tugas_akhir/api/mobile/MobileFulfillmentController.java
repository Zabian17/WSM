package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.MobileFulfillmentDetail;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.MobileFulfillmentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/v1/fulfillment/orders")
public class MobileFulfillmentController {
    private static final int MAX_PAGE_SIZE = 100;
    private final DeliveryOrderService deliveryOrderService;

    public MobileFulfillmentController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @GetMapping
    public MobilePage<MobileFulfillmentSummary> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<MobileFulfillmentSummary> result = deliveryOrderService.getMobileFulfillmentOrders(
                PageRequest.of(page, size));
        return new MobilePage<>(result.getContent(), new MobilePage.Pagination(result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{orderId}")
    public MobileFulfillmentDetail detail(@PathVariable Integer orderId) {
        return findOrder(orderId);
    }

    @PostMapping("/{orderId}/scans")
    public ScanResult scan(@PathVariable Integer orderId,
                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                           @RequestBody FulfillmentScan submission) {
        if (submission == null || submission.lineId() == null || submission.lineId() < 1
                || submission.quantity() == null || submission.quantity() < 1
                || !isBarcode(submission.deliveryOrderBarcode()) || !isBarcode(submission.itemBarcode())) {
            throw new IllegalArgumentException("Barcode DO, QR item, ID baris, dan jumlah scan wajib valid.");
        }
        if (!deliveryOrderService.recordMobileFulfillmentScan(orderId, submission.lineId(),
                submission.deliveryOrderBarcode(), submission.itemBarcode(), submission.quantity(), userId(user))) {
            throw new IllegalArgumentException("DO atau QR item tidak cocok dengan tahap dan reservasi.");
        }
        return new ScanResult(true);
    }

    @PostMapping("/{orderId}/complete")
    public StageCompletion complete(@PathVariable Integer orderId,
                                    @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        DeliveryOrderStatus status = deliveryOrderService.completeMobileFulfillment(orderId, userId(user));
        if (status == null) throw new IllegalStateException("Tahap tidak memiliki scan lengkap yang dapat diselesaikan.");
        return new StageCompletion(status);
    }

    @PostMapping("/{orderId}/dispatch")
    public DispatchCompletion dispatch(@PathVariable Integer orderId,
                                       @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        DeliveryOrderStatus status = deliveryOrderService.dispatchMobileFulfillment(orderId, userId(user));
        if (status == null) throw new IllegalStateException("DO belum LOADED atau Goods Issue gagal diposting.");
        return new DispatchCompletion(status);
    }

    private MobileFulfillmentDetail findOrder(Integer orderId) {
        Optional<MobileFulfillmentDetail> order = deliveryOrderService.getMobileFulfillmentOrder(orderId);
        return order.orElseThrow(MobileApiExceptions.NotFound::new);
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

    public record FulfillmentScan(Integer lineId, Integer quantity, String deliveryOrderBarcode,
                                  String itemBarcode) {}
    public record ScanResult(boolean recorded) {}
    public record StageCompletion(DeliveryOrderStatus status) {}
    public record DispatchCompletion(DeliveryOrderStatus status) {}
}
