package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.MaterialUsageStatus;
import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import com.mycompany.tugas_akhir.service.MaterialUsageService;
import com.mycompany.tugas_akhir.service.MaterialUsageService.MobilePickInstruction;
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
@RequestMapping("/api/mobile/v1/mro/tasks")
public class MobileMroController {
    private static final int MAX_PAGE_SIZE = 100;
    private final MaterialUsageService materialUsageService;

    public MobileMroController(MaterialUsageService materialUsageService) {
        this.materialUsageService = materialUsageService;
    }

    @GetMapping
    public MobilePage<MroTaskSummary> list(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<PickTask> result = materialUsageService.getMobilePickTasks(userId(user), PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toSummary).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{taskId}")
    public MroTaskDetail detail(@PathVariable Integer taskId,
                                @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        Integer pickerId = userId(user);
        PickTask task = requireAssignedTask(taskId, pickerId);
        return new MroTaskDetail(toSummary(task),
                materialUsageService.getMobilePickInstructions(taskId, pickerId));
    }

    @PostMapping("/{taskId}/scans")
    public ResponseEntity<MroScanResult> scan(@PathVariable Integer taskId,
                                              @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                              @RequestBody MroScan submission) {
        Integer pickerId = userId(user);
        requireAssignedTask(taskId, pickerId);
        if (submission == null || submission.quantity() == null || submission.quantity() < 1
                || !isBarcode(submission.itemBarcode()) || !isBarcode(submission.locationBarcode())) {
            throw new IllegalArgumentException("QR barang, lokasi, dan kuantitas scan wajib valid.");
        }
        if (!materialUsageService.recordMobilePick(taskId, submission.itemBarcode(),
                submission.locationBarcode(), submission.quantity(), pickerId)) {
            throw new IllegalArgumentException("Scan tidak cocok dengan barang, lot, lokasi, atau tugas MRO.");
        }
        return ResponseEntity.ok(new MroScanResult(true));
    }

    private PickTask requireAssignedTask(Integer taskId, Integer pickerId) {
        Optional<PickTask> task = materialUsageService.getMobilePickTask(taskId, pickerId);
        return task.orElseThrow(MobileApiExceptions.NotFound::new);
    }

    private MroTaskSummary toSummary(PickTask task) {
        return new MroTaskSummary(task.getId(), task.getTaskCode(), task.getMaterialUsage().getId(),
                task.getMaterialUsage().getRequestCode(), task.getMaterialUsage().getReason(),
                task.getStatus(), task.getMaterialUsage().getStatus(), task.getCreatedAt());
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

    public record MroTaskSummary(Integer taskId, String taskCode, Integer requestId, String requestCode,
                                 String reason, PickTaskStatus taskStatus, MaterialUsageStatus requestStatus,
                                 LocalDateTime createdAt) {}
    public record MroTaskDetail(MroTaskSummary task, List<MobilePickInstruction> lines) {}
    public record MroScan(Integer quantity, String itemBarcode, String locationBarcode) {}
    public record MroScanResult(boolean recorded) {}
}
