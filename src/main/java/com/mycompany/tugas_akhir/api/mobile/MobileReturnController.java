package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.ReturnRequest;
import com.mycompany.tugas_akhir.model.ReturnRequestLine;
import com.mycompany.tugas_akhir.model.ReturnStatus;
import com.mycompany.tugas_akhir.model.ReturnType;
import com.mycompany.tugas_akhir.service.ReturnRequestService;
import com.mycompany.tugas_akhir.service.ReturnRequestService.MobileReturnScan;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/v1/returns")
public class MobileReturnController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_LINES = 100;
    private final ReturnRequestService returnRequestService;

    public MobileReturnController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping
    public MobilePage<ReturnSummary> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        Page<ReturnRequest> result = returnRequestService.getMobileApprovedReturns(PageRequest.of(page, size));
        return new MobilePage<>(result.getContent().stream().map(this::toSummary).toList(),
                new MobilePage.Pagination(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{requestId}")
    public ReturnDetail detail(@PathVariable Integer requestId) {
        ReturnRequest request = requireApprovedReturn(requestId);
        return new ReturnDetail(toSummary(request), request.getLines().stream().map(this::toLine).toList());
    }

    @PostMapping("/{requestId}/post")
    public ResponseEntity<ReturnPosted> post(@PathVariable Integer requestId,
                                             @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user,
                                             @RequestBody ReturnScanSubmission submission) {
        ReturnRequest request = requireApprovedReturn(requestId);
        if (submission == null || submission.scans() == null || submission.scans().isEmpty()
                || submission.scans().size() > MAX_LINES || submission.scans().size() != request.getLines().size()) {
            throw new IllegalArgumentException("Kirim satu pasang scan untuk setiap baris return.");
        }
        Integer userId = userId(user);
        List<MobileReturnScan> scans = submission.scans().stream().map(scan -> {
            if (scan == null || scan.lineId() == null || scan.lineId() < 1 || !isBarcode(scan.itemBarcode())
                    || !isBarcode(scan.locationBarcode())) {
                throw new IllegalArgumentException("QR barang dan barcode lokasi wajib valid.");
            }
            return new MobileReturnScan(scan.lineId(), scan.itemBarcode(), scan.locationBarcode());
        }).toList();
        if (!returnRequestService.postApprovedReturnWithBarcodes(requestId, userId, scans)) {
            throw new IllegalArgumentException("Scan tidak cocok dengan barang, lot, lokasi, atau baris return.");
        }
        return ResponseEntity.ok(new ReturnPosted(ReturnStatus.POSTED));
    }

    private ReturnRequest requireApprovedReturn(Integer requestId) {
        Optional<ReturnRequest> request = returnRequestService.getMobileApprovedReturn(requestId);
        return request.orElseThrow(MobileApiExceptions.NotFound::new);
    }

    private ReturnSummary toSummary(ReturnRequest request) {
        return new ReturnSummary(request.getId(), request.getReturnCode(), request.getReturnType(),
                request.getReason(), request.getCreatedAt(), request.getLines().size());
    }

    private ReturnLine toLine(ReturnRequestLine line) {
        return new ReturnLine(line.getId(), line.getBarang().getKodeBarang(), line.getBarang().getNamaBarang(),
                line.getQuantity(), line.getBatchCode(), line.getExpiryDate(), line.getRak().getKodeRak());
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

    public record ReturnSummary(Integer id, String returnCode, ReturnType returnType, String reason,
                                LocalDateTime createdAt, int lineCount) {}
    public record ReturnDetail(ReturnSummary request, List<ReturnLine> lines) {}
    public record ReturnLine(Integer lineId, String itemCode, String itemName, Integer quantity,
                             String batchCode, LocalDate expiryDate, String rackCode) {}
    public record ReturnScanSubmission(List<ReturnScan> scans) {}
    public record ReturnScan(Integer lineId, String itemBarcode, String locationBarcode) {}
    public record ReturnPosted(ReturnStatus status) {}
}
