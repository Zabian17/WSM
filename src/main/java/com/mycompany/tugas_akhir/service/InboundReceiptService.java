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
import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.PurchaseOrderStatus;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InboundReceiptRepository;
import com.mycompany.tugas_akhir.repository.PurchaseOrderRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
public class InboundReceiptService {

    public record PutawayRecommendation(Rak rack, int availableCapacity, boolean categoryMatch,
                                        boolean storageTypeMatch,
                                        BigDecimal availableCbm) {
        public String getPreferenceLabel() {
            String storage = storageTypeMatch ? "Tipe storage cocok" : "Tipe umum";
            return storage + (categoryMatch ? " • Kategori cocok" : " • Lokasi umum/lainnya");
        }
    }

    private static final Path LABEL_DIRECTORY = Path.of(System.getProperty("user.home"), ".gudangku", "labels");
    @Value("${gudangku.inbound.qc-required:true}")
    private boolean qcRequired = true;
    @Value("${gudangku.inbound.qc-fifo:false}")
    private boolean qcFifoEnabled;
    private final InboundReceiptRepository receiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final BarangRepository barangRepository;
    private final SupplierRepository supplierRepository;
    private final RakRepository rakRepository;
    private final UserRepository userRepository;
    private final InboundStockPoster stockPoster;
    private final InventoryBalanceRepository balanceRepository;

    public InboundReceiptService(InboundReceiptRepository receiptRepository,
                                 PurchaseOrderRepository purchaseOrderRepository,
                                 BarangRepository barangRepository,
                                 SupplierRepository supplierRepository,
                                 RakRepository rakRepository,
                                 UserRepository userRepository,
                                 InboundStockPoster stockPoster,
                                 InventoryBalanceRepository balanceRepository) {
        this.receiptRepository = receiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.barangRepository = barangRepository;
        this.supplierRepository = supplierRepository;
        this.rakRepository = rakRepository;
        this.userRepository = userRepository;
        this.stockPoster = stockPoster;
        this.balanceRepository = balanceRepository;
    }

    @Transactional(readOnly = true)
    public List<InboundReceipt> getRecentReceipts() {
        if (qcFifoEnabled) {
            return receiptRepository.findWithPurchaseOrderQcFifo(InboundReceiptStatus.QC_PENDING,
                    PageRequest.of(0, 100));
        }
        return receiptRepository.findRecentWithPurchaseOrder(PageRequest.of(0, 100));
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
        InboundReceipt receipt = new InboundReceipt(code, barang, supplier, quantity,
                batch, expiryDate, notes, user, LocalDateTime.now());
        applyInitialQualityGate(receipt);
        return receiptRepository.save(receipt);
    }

    @Transactional
    public InboundReceipt createReceiptFromPurchaseOrder(Integer purchaseOrderLineId, int quantity,
                                                         String batchCode, LocalDate expiryDate,
                                                         String notes, Integer userId) {
        String batch = batchCode == null ? "" : batchCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (purchaseOrderLineId == null || userId == null || quantity <= 0
                || !batch.matches("[A-Z0-9._-]{1,64}") || "OPENING".equals(batch)) {
            return null;
        }
        PurchaseOrderLine referencedLine = purchaseOrderRepository.findLineById(purchaseOrderLineId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (referencedLine == null || user == null) return null;
        LockedPurchaseOrderLine lockedLine = lockPurchaseOrderLine(purchaseOrderLineId);
        if (lockedLine.order().getStatus() != PurchaseOrderStatus.APPROVED
                && lockedLine.order().getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED
                && lockedLine.order().getStatus() != PurchaseOrderStatus.OPEN) {
            return null;
        }
        PurchaseOrderLine line = lockedLine.line();
        if (quantity > line.getRemainingQuantity()) return null;

        line.setPendingQuantity(Math.addExact(line.getPendingQuantity(), quantity));
        String code = "RCV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                .toUpperCase(java.util.Locale.ROOT);
        InboundReceipt receipt = new InboundReceipt(code, line.getBarang(),
                line.getPurchaseOrder().getSupplier(), quantity, batch, expiryDate, notes, user,
                LocalDateTime.now());
        receipt.setPurchaseOrderLine(line);
        applyInitialQualityGate(receipt);
        return receiptRepository.save(receipt);
    }

    private void applyInitialQualityGate(InboundReceipt receipt) {
        if (!qcRequired) receipt.setStatus(InboundReceiptStatus.PUTAWAY_PENDING);
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
        if (receipt.getStatus() == InboundReceiptStatus.REJECTED) {
            releasePurchaseOrderPendingQuantity(receipt);
        }
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
        return completePutaway(receipt, rak, user);
    }

    @Transactional
    public boolean putawayWithScannedBarcodes(Integer receiptId, String itemQrPayload,
                                              String locationBarcode, Integer expectedRackId,
                                              Integer userId) {
        InboundReceipt receipt = receiptId == null ? null
                : receiptRepository.findByIdForUpdate(receiptId).orElse(null);
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (receipt == null || user == null || receipt.getStatus() != InboundReceiptStatus.PUTAWAY_PENDING
                || !matchesReceiptQr(receipt, itemQrPayload)) {
            return false;
        }

        String rackCode = decodeLocationBarcode(locationBarcode);
        if (rackCode == null) return false;
        Rak rack = rakRepository.findByKodeRak(rackCode).orElse(null);
        if (rack == null || !java.util.Objects.equals(rack.getId(), expectedRackId)) return false;
        return completePutaway(receipt, rack, user);
    }

    private boolean completePutaway(InboundReceipt receipt, Rak rak, User user) {
        if (receipt.getStatus() != InboundReceiptStatus.PUTAWAY_PENDING) return false;

        PurchaseOrder purchaseOrder = null;
        PurchaseOrderLine purchaseOrderLine = null;
        if (receipt.getPurchaseOrderLine() != null) {
            LockedPurchaseOrderLine lockedLine =
                    lockPurchaseOrderLine(receipt.getPurchaseOrderLine().getId());
            purchaseOrder = lockedLine.order();
            purchaseOrderLine = lockedLine.line();
        }
        if (purchaseOrderLine != null
                && (purchaseOrderLine.getPendingQuantity() < receipt.getQuantity()
                || receipt.getQuantity() > purchaseOrderLine.getOrderedQuantity()
                - purchaseOrderLine.getReceivedQuantity())) {
            throw new IllegalStateException("Kuantitas receipt tidak sesuai dengan sisa purchase order.");
        }

        boolean stored = stockPoster.postInbound(receipt.getBarang().getId(),
                receipt.getSupplier().getId(), rak.getId(), receipt.getBatchCode(), receipt.getExpiryDate(),
                receipt.getQuantity(), "Receiving " + receipt.getReceiptCode(), user.getId());
        if (!stored) return false;

        if (purchaseOrderLine != null) {
            boolean lineWasComplete = purchaseOrderLine.getReceivedQuantity()
                    == purchaseOrderLine.getOrderedQuantity();
            purchaseOrderLine.setPendingQuantity(
                    purchaseOrderLine.getPendingQuantity() - receipt.getQuantity());
            purchaseOrderLine.setReceivedQuantity(
                    Math.addExact(purchaseOrderLine.getReceivedQuantity(), receipt.getQuantity()));
            purchaseOrder.setReceivedQuantity(Math.addExact(
                    purchaseOrder.getReceivedQuantity(), receipt.getQuantity()));
            if (!lineWasComplete && purchaseOrderLine.getReceivedQuantity()
                    == purchaseOrderLine.getOrderedQuantity()) {
                purchaseOrder.setOpenLineCount(purchaseOrder.getOpenLineCount() - 1);
            }
            purchaseOrder.setStatus(purchaseOrder.getOpenLineCount() == 0
                    ? PurchaseOrderStatus.RECEIVED
                    : purchaseOrder.getReceivedQuantity() > 0
                    ? PurchaseOrderStatus.PARTIALLY_RECEIVED : PurchaseOrderStatus.OPEN);
            purchaseOrderRepository.save(purchaseOrder);
        }
        receipt.setPutawayRak(rak);
        receipt.setStatus(InboundReceiptStatus.PUTAWAY);
        receiptRepository.save(receipt);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<InboundReceipt> findReceipt(Integer receiptId) {
        return receiptRepository.findById(receiptId);
    }

    @Transactional(readOnly = true)
    public List<PutawayRecommendation> recommendPutawayLocations(Integer receiptId) {
        InboundReceipt receipt = receiptId == null ? null : receiptRepository.findById(receiptId).orElse(null);
        if (receipt == null || receipt.getStatus() != InboundReceiptStatus.PUTAWAY_PENDING
                || receipt.getQuantity() == null || receipt.getQuantity() <= 0) {
            return List.of();
        }

        String itemCategory = normalizeCategory(receipt.getBarang().getKategori());
        String itemStorageType = normalizeStorageType(receipt.getBarang().getStorageType());
        Map<Integer, BigDecimal> occupiedCbm = new HashMap<>();
        Set<Integer> unknownVolumeRacks = new HashSet<>();
        for (InventoryBalance balance : balanceRepository.findAllForLocationReport()) {
            if (balance.getRak() == null || balance.getQuantity() <= 0) continue;
            Barang storedItem = balance.getBarang();
            if (storedItem.getVolumeCbm() == null) {
                unknownVolumeRacks.add(balance.getRak().getId());
            } else {
                occupiedCbm.merge(balance.getRak().getId(), storedItem.getVolumeCbm()
                        .multiply(BigDecimal.valueOf(balance.getQuantity())), BigDecimal::add);
            }
        }
        return rakRepository.findAll().stream()
                .filter(rack -> rack.getSection() != null
                        && rack.getKapasitas() > 0
                        && matchesStorageType(rack, itemStorageType)
                        && rack.getKapasitas() - rack.getTerisi() >= receipt.getQuantity()
                        && (rack.getCapacityCbm() == null || receipt.getBarang().getVolumeCbm() == null
                        || (!unknownVolumeRacks.contains(rack.getId())
                        && receipt.getBarang().getVolumeCbm().multiply(BigDecimal.valueOf(receipt.getQuantity()))
                        .add(occupiedCbm.getOrDefault(rack.getId(), BigDecimal.ZERO))
                        .compareTo(rack.getCapacityCbm()) <= 0)))
                .map(rack -> new PutawayRecommendation(rack,
                        rack.getKapasitas() - rack.getTerisi(),
                        matchesCategory(rack, itemCategory), isExactStorageType(rack, itemStorageType),
                        rack.getCapacityCbm() == null
                        || receipt.getBarang().getVolumeCbm() == null ? null
                        : rack.getCapacityCbm().subtract(occupiedCbm.getOrDefault(rack.getId(), BigDecimal.ZERO))))
                .sorted(Comparator
                        .comparingInt((PutawayRecommendation recommendation) ->
                                storageTypeRank(recommendation, itemStorageType))
                        .thenComparingInt((PutawayRecommendation recommendation) ->
                                categoryRank(recommendation, itemCategory))
                        .thenComparingInt(recommendation -> recommendation.rack().getSection().getPutawayPriority())
                        .thenComparingDouble(this::putawayUtilization)
                        .thenComparing(recommendation -> recommendation.rack().getKodeRak()))
                .toList();
    }

    private boolean matchesStorageType(Rak rack, String itemStorageType) {
        if (itemStorageType.isEmpty()) return true;
        String rackType = normalizeStorageType(rack.getSection().getStorageType());
        return rackType.isEmpty() || "GENERAL".equals(rackType) || "ANY".equals(rackType)
                || rackType.equals(itemStorageType);
    }

    private boolean isExactStorageType(Rak rack, String itemStorageType) {
        return !itemStorageType.isEmpty()
                && normalizeStorageType(rack.getSection().getStorageType()).equals(itemStorageType);
    }

    private int storageTypeRank(PutawayRecommendation recommendation, String itemStorageType) {
        if (itemStorageType.isEmpty()) return 1;
        return recommendation.storageTypeMatch() ? 0 : 1;
    }

    private double putawayUtilization(PutawayRecommendation recommendation) {
        Rak rack = recommendation.rack();
        if (recommendation.availableCbm() != null && rack.getCapacityCbm() != null
                && rack.getCapacityCbm().signum() > 0) {
            return 1d - recommendation.availableCbm().doubleValue() / rack.getCapacityCbm().doubleValue();
        }
        return (double) rack.getTerisi() / rack.getKapasitas();
    }

    public Path generateQrLabel(Integer receiptId) throws IOException {
        return generateQrLabel(receiptId, LABEL_DIRECTORY);
    }

    Path generateQrLabel(Integer receiptId, Path labelDirectory) throws IOException {
        InboundReceipt receipt = receiptRepository.findById(receiptId).orElseThrow(
                () -> new IOException("Dokumen receiving tidak ditemukan."));
        Files.createDirectories(labelDirectory);
        Path target = labelDirectory.resolve("receipt-" + receipt.getReceiptCode() + ".png");
        String payload = itemQrPayload(receipt.getBarang().getKodeBarang(), receipt.getBatchCode(),
                receipt.getExpiryDate());
        writeQrLabel(payload, target);
        return target;
    }

    public Path generateItemQrLabel(String sku, String batchCode, LocalDate expiryDate) throws IOException {
        return generateItemQrLabel(sku, batchCode, expiryDate, LABEL_DIRECTORY);
    }

    Path generateItemQrLabel(String sku, String batchCode, LocalDate expiryDate,
                             Path labelDirectory) throws IOException {
        if (sku == null || sku.isBlank() || sku.length() > 64
                || batchCode == null || batchCode.isBlank() || batchCode.length() > 64) {
            throw new IOException("SKU atau batch untuk QR item tidak valid.");
        }
        String payload = itemQrPayload(sku, batchCode, expiryDate);
        String fileHash;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            fileHash = java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("Algoritma hash untuk nama file QR tidak tersedia.", exception);
        }
        Files.createDirectories(labelDirectory);
        Path target = labelDirectory.resolve("item-" + fileHash + ".png");
        if (Files.isRegularFile(target)) return target;
        writeQrLabel(payload, target);
        return target;
    }

    private String itemQrPayload(String sku, String batchCode, LocalDate expiryDate) {
        return "WMS1|" + encode(sku) + "|" + encode(batchCode)
                + "|" + (expiryDate == null ? "-" : expiryDate);
    }

    private void writeQrLabel(String payload, Path target) throws IOException {
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
    }

    public Path generateLocationQrLabel(Integer rackId) throws IOException {
        return generateLocationQrLabel(rackId, LABEL_DIRECTORY);
    }

    Path generateLocationQrLabel(Integer rackId, Path labelDirectory) throws IOException {
        Rak rack = rackId == null ? null : rakRepository.findById(rackId).orElse(null);
        if (rack == null) throw new IOException("Lokasi rak tidak ditemukan.");
        Files.createDirectories(labelDirectory);
        Path target = labelDirectory.resolve("location-" + rack.getKodeRak() + ".png");
        String payload = "SLOC1|" + encode(rack.getKodeRak());
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(payload, BarcodeFormat.QR_CODE, 360, 360, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", target);
        } catch (com.google.zxing.WriterException e) {
            throw new IOException("QR lokasi gagal dibuat.", e);
        }
        return target;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesReceiptQr(InboundReceipt receipt, String payload) {
        BarcodePayload.Item item = BarcodePayload.parseItem(payload);
        if (item == null || !receipt.getBarang().getKodeBarang().equalsIgnoreCase(item.sku())
                || !receipt.getBatchCode().equalsIgnoreCase(item.batchCode())) {
            return false;
        }
        return java.util.Objects.equals(receipt.getExpiryDate(), item.expiryDate());
    }

    private String decodeLocationBarcode(String barcode) {
        return BarcodePayload.parseLocation(barcode);
    }

    private String appendNote(String notes, String additional) {
        return notes == null || notes.isBlank() ? additional : notes + "\n" + additional;
    }

    private boolean matchesCategory(Rak rack, String itemCategory) {
        String targetCategory = normalizeCategory(rack.getSection().getTargetKategori());
        return !targetCategory.isEmpty() && !"semua".equals(targetCategory)
                && targetCategory.equals(itemCategory);
    }

    private int categoryRank(PutawayRecommendation recommendation, String itemCategory) {
        String targetCategory = normalizeCategory(recommendation.rack().getSection().getTargetKategori());
        if (!targetCategory.isEmpty() && !"semua".equals(targetCategory)
                && targetCategory.equals(itemCategory)) return 0;
        if (targetCategory.isEmpty() || "semua".equals(targetCategory)) return 1;
        return 2;
    }

    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeStorageType(String storageType) {
        return storageType == null ? "" : storageType.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void releasePurchaseOrderPendingQuantity(InboundReceipt receipt) {
        if (receipt.getPurchaseOrderLine() == null) return;
        PurchaseOrderLine line = lockPurchaseOrderLine(receipt.getPurchaseOrderLine().getId()).line();
        if (line.getPendingQuantity() < receipt.getQuantity()) {
            throw new IllegalStateException("Kuantitas pending purchase order tidak konsisten.");
        }
        line.setPendingQuantity(line.getPendingQuantity() - receipt.getQuantity());
    }

    private LockedPurchaseOrderLine lockPurchaseOrderLine(Integer lineId) {
        PurchaseOrderLine reference = purchaseOrderRepository.findLineById(lineId)
                .orElseThrow(() -> new IllegalStateException("Baris purchase order tidak ditemukan."));
        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(reference.getPurchaseOrder().getId())
                .orElseThrow(() -> new IllegalStateException("Purchase order tidak ditemukan."));
        PurchaseOrderLine line = purchaseOrderRepository.findLineByIdForUpdate(lineId)
                .orElseThrow(() -> new IllegalStateException("Baris purchase order tidak ditemukan."));
        return new LockedPurchaseOrderLine(order, line);
    }

    private record LockedPurchaseOrderLine(PurchaseOrder order, PurchaseOrderLine line) {}
}
