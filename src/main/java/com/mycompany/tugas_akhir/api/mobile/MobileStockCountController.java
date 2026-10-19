package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.StockCount;
import com.mycompany.tugas_akhir.model.StockCountStatus;
import com.mycompany.tugas_akhir.repository.StockCountLineProjection;
import com.mycompany.tugas_akhir.repository.StockCountReviewLineProjection;
import com.mycompany.tugas_akhir.repository.StockCountSummaryProjection;
import com.mycompany.tugas_akhir.service.StockCountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/v1/stock-counts")
public class MobileStockCountController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SUBMISSION_LINES = 5_000;
    private final StockCountService stockCountService;

    public MobileStockCountController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @GetMapping
    public MobilePage<MobileStockCountSummary> list(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<StockCountSummaryProjection> result = stockCountService.getMobileActiveCounts(
                userId(user), PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toSummary).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/{countId}/start")
    public MobileStockCountSummary start(@PathVariable Integer countId) {
        StockCountStatus status = requireCountStatus(countId);
        if (status != StockCountStatus.SCHEDULED) {
            throw new IllegalStateException("Hanya jadwal opname yang dapat dimulai.");
        }
        return toSummary(stockCountService.startScheduledCount(countId));
    }

    @GetMapping("/{countId}/lines")
    public MobilePage<MobileStockCountLine> lines(@PathVariable Integer countId,
                                                  @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int size) {
        requireCounting(countId);
        validatePage(page, size);
        Page<StockCountLineProjection> result = stockCountService.getMobileCountLines(
                countId, userId(user), PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toLine).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{countId}/review")
    public MobilePage<MobileStockCountReviewLine> review(@PathVariable Integer countId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "100") int size) {
        if (requireCountStatus(countId) != StockCountStatus.COUNTED) {
            throw new IllegalStateException("Hasil opname baru dapat ditinjau setelah seluruh SKU dihitung.");
        }
        validatePage(page, size);
        Page<StockCountReviewLineProjection> result = stockCountService.getMobileCountReviewLines(
                countId, PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toReviewLine).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/{countId}/post")
    public CountPostResult post(@PathVariable Integer countId,
                                @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        if (requireCountStatus(countId) != StockCountStatus.COUNTED) {
            throw new IllegalStateException("Hanya sesi opname yang sudah ditinjau lengkap dapat diposting.");
        }
        stockCountService.postCount(countId, userId(user));
        return new CountPostResult(StockCountStatus.POSTED);
    }

    @PutMapping("/{countId}/lines")
    public ResponseEntity<SubmissionResult> submit(@PathVariable Integer countId,
                                                    @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                                    @RequestBody CountSubmission submission) {
        requireCounting(countId);
        if (submission == null || submission.counts() == null || submission.counts().isEmpty()
                || submission.counts().size() > MAX_SUBMISSION_LINES) {
            throw new IllegalArgumentException("Kirim 1 sampai " + MAX_SUBMISSION_LINES + " baris hitungan.");
        }
        Integer userId = userId(user);
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (CountEntry entry : submission.counts()) {
            if (entry == null || entry.lineId() == null || entry.quantity() == null || entry.quantity() < 0
                    || quantities.putIfAbsent(entry.lineId(), entry.quantity()) != null) {
                throw new IllegalArgumentException("Baris hitungan memiliki ID atau jumlah yang tidak valid.");
            }
            if (entry.barcodePayload() != null) {
                stockCountService.validateMobileCountBarcode(countId, entry.lineId(), userId,
                        entry.barcodePayload());
            }
        }
        stockCountService.submitCounts(countId, quantities, userId);
        return ResponseEntity.ok(new SubmissionResult(stockCountService.getCountStatus(countId)));
    }

    private StockCountStatus requireCountStatus(Integer countId) {
        try {
            return stockCountService.getCountStatus(countId);
        } catch (IllegalArgumentException exception) {
            throw new MobileApiExceptions.NotFound();
        }
    }

    private void requireCounting(Integer countId) {
        if (requireCountStatus(countId) != StockCountStatus.COUNTING) {
            throw new IllegalStateException("Sesi opname tidak sedang menerima hasil hitung.");
        }
    }

    private Integer userId(OAuth2AuthenticatedPrincipal user) {
        Object id = user == null ? null : user.getAttribute("userId");
        if (id instanceof Integer userId) return userId;
        throw new MobileApiExceptions.InvalidCredentials();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page harus >= 0 dan size harus antara 1 dan "
                    + MAX_PAGE_SIZE + ".");
        }
    }

    private MobileStockCountSummary toSummary(StockCountSummaryProjection count) {
        return new MobileStockCountSummary(count.id(), count.countCode(), count.cycleCode(),
                count.cycleType(), count.rackCode(), count.status(), count.dueDate(), count.createdAt());
    }

    private MobileStockCountSummary toSummary(StockCount count) {
        return new MobileStockCountSummary(count.getId(), count.getCountCode(), count.getCycleCode(),
                count.getCycleType(), count.getRak().getKodeRak(), count.getStatus(),
                count.getDueDate(), count.getCreatedAt());
    }

    private MobileStockCountLine toLine(StockCountLineProjection line) {
        return new MobileStockCountLine(line.id(), line.itemCode(), line.itemName(), line.unit(), line.assignedTo());
    }

    private MobileStockCountReviewLine toReviewLine(StockCountReviewLineProjection line) {
        return new MobileStockCountReviewLine(line.id(), line.itemCode(), line.itemName(), line.unit(),
                line.expectedQuantity(), line.countedQuantity(), line.countedQuantity() - line.expectedQuantity());
    }

    public record CountSubmission(List<CountEntry> counts) {}
    public record CountEntry(Integer lineId, Integer quantity, String barcodePayload) {}
    public record SubmissionResult(StockCountStatus status) {}
    public record MobileStockCountReviewLine(Integer id, String itemCode, String itemName, String unit,
                                             Integer expectedQuantity, Integer countedQuantity, Integer variance) {}
    public record CountPostResult(StockCountStatus status) {}
}
