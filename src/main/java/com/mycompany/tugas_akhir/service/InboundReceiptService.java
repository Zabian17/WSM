package com.mycompany.tugas_akhir.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.InboundReceiptStatus;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InboundReceiptRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InboundReceiptService {

    private static final Path LABEL_DIRECTORY = Path.of(System.getProperty("user.home"), ".gudangku", "labels");
    private final InboundReceiptRepository receiptRepository;
    private final BarangRepository barangRepository;
    private final SupplierRepository supplierRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InboundStockPoster stockPoster;

    public InboundReceiptService(InboundReceiptRepository receiptRepository,
                                 BarangRepository barangRepository,
                                 SupplierRepository supplierRepository,
                                 RakRepository rakRepository,
                                 UserRepository userRepository,
                                 InboundStockPoster stockPoster) {
        this.receiptRepository = receiptRepository;
        this.barangRepository = barangRepository;
        this.supplierRepository = supplierRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.stockPoster = stockPoster;
    }

    @Transactional(readOnly = true)
    public List<InboundReceipt> getRecentReceipts() {
        return receiptRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional
    public InboundReceipt createReceipt(Integer barangId, Integer supplierId, int quantity,
                                        String batchCode, LocalDate expiryDate, String notes, Integer userId) {
        String batch = batchCode == null ? "" : batchCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (barangId == null || supplierId == null || userId == null || quantity <= 0
                || !batch.matches("[A-Z0-9._-]{1,64}")
                || "OPENING".equals(batch)) {
            return null;
        }

        Barang barang = barangRepository.findById(barangId).orElse(null);
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (barang == null || supplier == null || user == null) return null;

        String code = "RCV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                .toUpperCase(java.util.Locale.ROOT);
        return receiptRepository.save(new InboundReceipt(code, barang, supplier, quantity,
                batch, expiryDate, notes, user, LocalDateTime.now()));
    }

    @Transactional
    public boolean completeQualityCheck(Integer receiptId, Integer userId, boolean accepted, String notes) {
        InboundReceipt receipt = receiptRepository.findByIdForUpdate(receiptId).orElse(null);
        User qcUser = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (receipt == null || qcUser == null || receipt.getStatus() != InboundReceiptStatus.QC_PENDING) {
            return false;
        }

        boolean expired = receipt.getExpiryDate() != null && receipt.getExpiryDate().isBefore(LocalDate.now());
        receipt.setStatus(accepted && !expired
                ? InboundReceiptStatus.PUTAWAY_PENDING : InboundReceiptStatus.REJECTED);
        receipt.setQcBy(qcUser);
        receipt.setQcAt(LocalDateTime.now());
        receipt.setQcNotes(expired ? appendNote(notes, "Ditolak: tanggal kedaluwarsa sudah lewat.") : notes);
        receiptRepository.save(receipt);
        return true;
    }

    @Transactional
    public boolean putaway(Integer receiptId, Integer rakId, Integer userId) {
        InboundReceipt receipt = receiptRepository.findByIdForUpdate(receiptId).orElse(null);
        Rak rak = rakId == null ? null : rakRepository.findById(rakId).orElse(null);
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (receipt == null || rak == null || user == null
                || receipt.getStatus() != InboundReceiptStatus.PUTAWAY_PENDING) {
            return false;
        }

        boolean stored = stockPoster.postInbound(receipt.getBarang().getId(),
                receipt.getSupplier().getId(), rak.getId(), receipt.getBatchCode(), receipt.getExpiryDate(),
                receipt.getQuantity(), "Receiving " + receipt.getReceiptCode(), user.getId());
        if (!stored) return false;

        receipt.setPutawayRak(rak);
        receipt.setStatus(InboundReceiptStatus.PUTAWAY);
        receiptRepository.save(receipt);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<InboundReceipt> findReceipt(Integer receiptId) {
        return receiptRepository.findById(receiptId);
    }

    public Path generateQrLabel(Integer receiptId) throws IOException {
        return generateQrLabel(receiptId, LABEL_DIRECTORY);
    }

    Path generateQrLabel(Integer receiptId, Path labelDirectory) throws IOException {
        InboundReceipt receipt = receiptRepository.findById(receiptId).orElseThrow(
                () -> new IOException("Dokumen receiving tidak ditemukan."));
        Files.createDirectories(labelDirectory);
        Path target = labelDirectory.resolve("receipt-" + receipt.getReceiptCode() + ".png");

        String payload = "WMS1|" + encode(receipt.getBarang().getKodeBarang())
                + "|" + encode(receipt.getBatchCode())
                + "|" + (receipt.getExpiryDate() == null ? "-" : receipt.getExpiryDate());
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        BitMatrix matrix;
        try {
            matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360, hints);
        } catch (com.google.zxing.WriterException e) {
            throw new IOException("QR label gagal dibuat.", e);
        }
        MatrixToImageWriter.writeToPath(matrix, "PNG", target);
        return target;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String appendNote(String notes, String additional) {
        return notes == null || notes.isBlank() ? additional : notes + "\n" + additional;
    }
}