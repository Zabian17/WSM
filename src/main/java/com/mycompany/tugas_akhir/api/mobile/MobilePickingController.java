package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import com.mycompany.tugas_akhir.service.DeliveryOrderService;
import com.mycompany.tugas_akhir.service.DeliveryOrderService.MobilePickInstruction;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/v1/picking/tasks")
public class MobilePickingController {
    private static final int MAX_PAGE_SIZE = 100;
    private final DeliveryOrderService deliveryOrderService;

    public MobilePickingController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @GetMapping
    public MobilePage<PickTaskSummary> list(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<PickTask> result = deliveryOrderService.getMobilePickTasks(userId(user), PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toSummary).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{taskId}")
    public PickTaskDetail detail(@PathVariable Integer taskId,
                                 @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        Integer pickerId = userId(user);
        PickTask task = requireAssignedTask(taskId, pickerId);
        return new PickTaskDetail(toSummary(task), deliveryOrderService.getMobilePickInstructions(taskId, pickerId));
    }

    @PostMapping("/{taskId}/scans")
    public ResponseEntity<PickScanResult> scan(@PathVariable Integer taskId,
                                               @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                               @RequestBody PickScan submission) {
        Integer pickerId = userId(user);
        requireAssignedTask(taskId, pickerId);
        if (submission == null || submission.lineId() == null || submission.lineId() < 1
                || submission.quantity() == null || submission.quantity() < 1
                || !isBarcode(submission.itemBarcode()) || !isBarcode(submission.locationBarcode())) {
            throw new IllegalArgumentException("SKU, lokasi, ID baris, dan kuantitas scan wajib valid.");
        }
        boolean recorded = deliveryOrderService.recordMobilePick(taskId, submission.lineId(),
                submission.itemBarcode(), submission.locationBarcode(), submission.quantity(), pickerId);
        if (!recorded) throw new IllegalArgumentException("Scan tidak cocok dengan item, lot, lokasi, atau tugas.");
        return ResponseEntity.ok(new PickScanResult(true));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<PickCompletion> complete(@PathVariable Integer taskId,
                                                   @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        Integer pickerId = userId(user);
        requireAssignedTask(taskId, pickerId);
        if (!deliveryOrderService.completeMobilePicking(taskId, pickerId)) {
            throw new IllegalStateException("Picking belum memiliki hasil scan yang dapat diselesaikan.");
        }
        return ResponseEntity.ok(new PickCompletion(DeliveryOrderStatus.PICKED));
    }

    private PickTask requireAssignedTask(Integer taskId, Integer pickerId) {
        Optional<PickTask> task = deliveryOrderService.getMobilePickTask(taskId, pickerId);
        return task.orElseThrow(MobileApiExceptions.NotFound::new);
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

    private PickTaskSummary toSummary(PickTask task) {
        return new PickTaskSummary(task.getId(), task.getTaskCode(), task.getDeliveryOrder().getId(),
                task.getDeliveryOrder().getOrderCode(), task.getAssignedSection().getKodeSection(),
                task.getStatus(), task.getCreatedAt());
    }

    public record PickTaskSummary(Integer taskId, String taskCode, Integer orderId, String orderCode,
                                  String sectionCode, PickTaskStatus status,
                                  LocalDateTime createdAt) {}
    public record PickTaskDetail(PickTaskSummary task, List<MobilePickInstruction> lines) {}
    public record PickScan(Integer lineId, Integer quantity, String itemBarcode, String locationBarcode) {}
    public record PickScanResult(boolean recorded) {}
    public record PickCompletion(DeliveryOrderStatus status) {}
}
