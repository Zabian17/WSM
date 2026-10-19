package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.InboundReceipt;
import com.mycompany.tugas_akhir.model.InboundReceiptStatus;
import com.mycompany.tugas_akhir.model.InventoryBalance;
import com.mycompany.tugas_akhir.model.PurchaseOrder;
import com.mycompany.tugas_akhir.model.PurchaseOrderLine;
import com.mycompany.tugas_akhir.model.PurchaseOrderStatus;
import com.mycompany.tugas_akhir.model.Rak;
import com.mycompany.tugas_akhir.model.Section;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.model.User;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.InboundReceiptRepository;
import com.mycompany.tugas_akhir.repository.PurchaseOrderRepository;
import com.mycompany.tugas_akhir.repository.RakRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundReceiptServiceTest {

    @Mock private InboundReceiptRepository receiptRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InboundStockPoster stockPoster;
    @Mock private InventoryBalanceRepository balanceRepository;

    @TempDir Path tempDirectory;

    private InboundReceiptService service;

    @BeforeEach
    void setUp() {
        service = new InboundReceiptService(receiptRepository, purchaseOrderRepository, barangRepository, supplierRepository,
            rakRepository, userRepository, stockPoster, balanceRepository);
    }

    @Test
    void createdReceiptWaitsForQcAndDoesNotChangeStock() {
        Barang barang = Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build();
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User creator = User.builder().id(3).fullName("Receiver").build();
        when(barangRepository.findById(1)).thenReturn(Optional.of(barang));
        when(supplierRepository.findById(2)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(3)).thenReturn(Optional.of(creator));
        when(receiptRepository.save(any(InboundReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InboundReceipt receipt = service.createReceipt(1, 2, 5, "lot-01", null, "sealed", 3);

        assertNotNull(receipt);
        assertEquals("LOT-01", receipt.getBatchCode());
        assertEquals(InboundReceiptStatus.QC_PENDING, receipt.getStatus());
        verifyNoInteractions(stockPoster);
    }

    @Test
    void configuredQcFifoLoadsOldestPendingReceiptsBeforeRecentReceipts() {
        ReflectionTestUtils.setField(service, "qcFifoEnabled", true);
        when(receiptRepository.findWithPurchaseOrderQcFifo(
                eq(InboundReceiptStatus.QC_PENDING), any())).thenReturn(java.util.List.of());

        assertTrue(service.getRecentReceipts().isEmpty());

        verify(receiptRepository).findWithPurchaseOrderQcFifo(eq(InboundReceiptStatus.QC_PENDING), any());
        verify(receiptRepository, never()).findRecentWithPurchaseOrder(any());
    }

    @Test
    void configuredQcBypassSendsReceiptDirectlyToPutawayWithoutPostingStock() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "qcRequired", false);
        Barang barang = Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build();
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User creator = User.builder().id(3).fullName("Receiver").build();
        when(barangRepository.findById(1)).thenReturn(Optional.of(barang));
        when(supplierRepository.findById(2)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(3)).thenReturn(Optional.of(creator));
        when(receiptRepository.save(any(InboundReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InboundReceipt receipt = service.createReceipt(1, 2, 5, "lot-01", null, "sealed", 3);

        assertNotNull(receipt);
        assertEquals(InboundReceiptStatus.PUTAWAY_PENDING, receipt.getStatus());
        assertNull(receipt.getQcBy());
        assertNull(receipt.getQcAt());
        verifyNoInteractions(stockPoster);
    }

    @Test
    void qcCannotApproveExpiredGoods() {
        User qcUser = User.builder().id(4).fullName("QC").build();
        InboundReceipt receipt = receipt(InboundReceiptStatus.QC_PENDING,
                LocalDate.now().minusDays(1));
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(4)).thenReturn(Optional.of(qcUser));

        assertTrue(service.completeQualityCheck(8, 4, true, "looks okay"));

        assertEquals(InboundReceiptStatus.REJECTED, receipt.getStatus());
        assertTrue(receipt.getQcNotes().contains("kedaluwarsa"));
        verifyNoInteractions(stockPoster);
    }

    @Test
    void putawayOnlyPostsStockAfterQcPasses() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING, LocalDate.now().plusDays(30));
        User operator = User.builder().id(4).fullName("Operator").build();
        Rak rack = new Rak(5, null, "A-01", 100, 0);
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findById(5)).thenReturn(Optional.of(rack));
        when(userRepository.findById(4)).thenReturn(Optional.of(operator));
        when(stockPoster.postInbound(1, 2, 5, "LOT-01", LocalDate.now().plusDays(30),
                5, "Receiving " + receipt.getReceiptCode(), 4)).thenReturn(true);

        assertTrue(service.putaway(8, 5, 4));

        assertEquals(InboundReceiptStatus.PUTAWAY, receipt.getStatus());
        assertEquals(rack, receipt.getPutawayRak());
        verify(stockPoster).postInbound(1, 2, 5, "LOT-01", LocalDate.now().plusDays(30),
                5, "Receiving " + receipt.getReceiptCode(), 4);
        verify(receiptRepository).save(receipt);
    }

    @Test
    void scannedItemAndLocationBarcodesAreValidatedBeforePutaway() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING,
                LocalDate.now().plusDays(30));
        User operator = User.builder().id(4).fullName("Operator").build();
        Rak rack = new Rak(5, section("A", "Semua"), "A-01", 100, 0);
        Rak otherRack = new Rak(6, section("B", "Semua"), "B-01", 100, 0);
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(4)).thenReturn(Optional.of(operator));
        when(rakRepository.findByKodeRak("B-01")).thenReturn(Optional.of(otherRack));
        when(rakRepository.findByKodeRak("A-01")).thenReturn(Optional.of(rack));
        when(stockPoster.postInbound(1, 2, 5, "LOT-01", receipt.getExpiryDate(),
                5, "Receiving " + receipt.getReceiptCode(), 4)).thenReturn(true);

        String correctLocation = locationPayload("A-01");
        assertFalse(service.putawayWithScannedBarcodes(8,
                itemPayload("WRONG-SKU", "LOT-01", receipt.getExpiryDate()),
                correctLocation, 5, 4));
        assertFalse(service.putawayWithScannedBarcodes(8,
                itemPayload("BRG-1", "WRONG-BATCH", receipt.getExpiryDate()),
                correctLocation, 5, 4));
        assertFalse(service.putawayWithScannedBarcodes(8,
                itemPayload("BRG-1", "LOT-01", receipt.getExpiryDate()),
                locationPayload("B-01"), 5, 4));
        assertFalse(service.putawayWithScannedBarcodes(8,
                itemPayload("BRG-1", "LOT-01", LocalDate.now().plusDays(31)),
                correctLocation, 5, 4));

        assertTrue(service.putawayWithScannedBarcodes(8,
                itemPayload("BRG-1", "LOT-01", receipt.getExpiryDate()),
                correctLocation, 5, 4));

        assertEquals(InboundReceiptStatus.PUTAWAY, receipt.getStatus());
        assertEquals(rack, receipt.getPutawayRak());
        verify(stockPoster, times(1)).postInbound(1, 2, 5, "LOT-01", receipt.getExpiryDate(),
                5, "Receiving " + receipt.getReceiptCode(), 4);
    }

    @Test
    void locationQrLabelContainsScannableRackCode() throws Exception {
        Rak rack = new Rak(5, section("A", "Semua"), "A-01", 100, 0);
        when(rakRepository.findById(5)).thenReturn(Optional.of(rack));

        Path label = service.generateLocationQrLabel(5, tempDirectory);

        var image = ImageIO.read(label.toFile());
        String payload = new MultiFormatReader().decode(new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
        assertTrue(Files.isRegularFile(label));
        assertEquals("SLOC1|" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("A-01".getBytes(java.nio.charset.StandardCharsets.UTF_8)), payload);
    }

    @Test
    void poReceiptReservesOnlyRemainingOrderedQuantity() {
        PurchaseOrderLine line = purchaseOrderLine(10, 10, 7);
        line.setPendingQuantity(2);
        User receiver = User.builder().id(4).fullName("Receiver").build();
        when(purchaseOrderRepository.findLineById(10)).thenReturn(Optional.of(line));
        when(purchaseOrderRepository.findByIdForUpdate(20)).thenReturn(Optional.of(line.getPurchaseOrder()));
        when(purchaseOrderRepository.findLineByIdForUpdate(10)).thenReturn(Optional.of(line));
        when(userRepository.findById(4)).thenReturn(Optional.of(receiver));
        when(receiptRepository.save(any(InboundReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InboundReceipt receipt = service.createReceiptFromPurchaseOrder(
                10, 1, "LOT-PO", null, null, 4);
        InboundReceipt overReceipt = service.createReceiptFromPurchaseOrder(
                10, 2, "LOT-OVER", null, null, 4);

        assertNotNull(receipt);
        assertEquals(line, receipt.getPurchaseOrderLine());
        assertNull(overReceipt);
        assertEquals(3, line.getPendingQuantity());
    }

    @Test
    void rejectedPoReceiptReleasesPendingQuantityForReplacement() {
        PurchaseOrderLine line = purchaseOrderLine(10, 10, 4);
        line.setPendingQuantity(4);
        InboundReceipt receipt = receipt(InboundReceiptStatus.QC_PENDING,
                LocalDate.now().plusDays(20), 4);
        receipt.setPurchaseOrderLine(line);
        User qcUser = User.builder().id(4).fullName("QC").build();
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(4)).thenReturn(Optional.of(qcUser));
        when(purchaseOrderRepository.findLineById(10)).thenReturn(Optional.of(line));
        when(purchaseOrderRepository.findByIdForUpdate(20)).thenReturn(Optional.of(line.getPurchaseOrder()));
        when(purchaseOrderRepository.findLineByIdForUpdate(10)).thenReturn(Optional.of(line));

        assertTrue(service.completeQualityCheck(8, 4, false, "Barang rusak"));

        assertEquals(InboundReceiptStatus.REJECTED, receipt.getStatus());
        assertEquals(0, line.getPendingQuantity());
    }

    @Test
    void poPutawayUpdatesReceivedQuantityAndCompletesOrder() {
        PurchaseOrderLine line = purchaseOrderLine(10, 5, 0);
        line.setPendingQuantity(5);
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING, LocalDate.now().plusDays(30));
        receipt.setPurchaseOrderLine(line);
        User operator = User.builder().id(4).fullName("Operator").build();
        Rak rack = new Rak(5, null, "A-01", 100, 0);
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findById(5)).thenReturn(Optional.of(rack));
        when(userRepository.findById(4)).thenReturn(Optional.of(operator));
        when(purchaseOrderRepository.findLineById(10)).thenReturn(Optional.of(line));
        when(purchaseOrderRepository.findLineByIdForUpdate(10)).thenReturn(Optional.of(line));
        when(stockPoster.postInbound(1, 2, 5, "LOT-01", LocalDate.now().plusDays(30),
                5, "Receiving " + receipt.getReceiptCode(), 4)).thenReturn(true);
        when(purchaseOrderRepository.findByIdForUpdate(20)).thenReturn(Optional.of(line.getPurchaseOrder()));
        assertTrue(service.putaway(8, 5, 4));

        assertEquals(5, line.getReceivedQuantity());
        assertEquals(0, line.getPendingQuantity());
        assertEquals(PurchaseOrderStatus.RECEIVED, line.getPurchaseOrder().getStatus());
    }

    @Test
    void partialPoPutawayLeavesOrderOpenForRemainingQuantity() {
        PurchaseOrderLine line = purchaseOrderLine(10, 8, 0);
        line.setPendingQuantity(5);
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING,
                LocalDate.now().plusDays(30), 5);
        receipt.setPurchaseOrderLine(line);
        User operator = User.builder().id(4).fullName("Operator").build();
        Rak rack = new Rak(5, null, "A-01", 100, 0);
        when(receiptRepository.findByIdForUpdate(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findById(5)).thenReturn(Optional.of(rack));
        when(userRepository.findById(4)).thenReturn(Optional.of(operator));
        when(purchaseOrderRepository.findLineById(10)).thenReturn(Optional.of(line));
        when(purchaseOrderRepository.findLineByIdForUpdate(10)).thenReturn(Optional.of(line));
        when(stockPoster.postInbound(1, 2, 5, "LOT-01", LocalDate.now().plusDays(30),
                5, "Receiving " + receipt.getReceiptCode(), 4)).thenReturn(true);
        when(purchaseOrderRepository.findByIdForUpdate(20)).thenReturn(Optional.of(line.getPurchaseOrder()));
        assertTrue(service.putaway(8, 5, 4));

        assertEquals(5, line.getReceivedQuantity());
        assertEquals(3, line.getRemainingQuantity());
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, line.getPurchaseOrder().getStatus());
    }

    @Test
    void putawayRecommendationsPrioritizeCategoryAndAvailableCapacity() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING,
                LocalDate.now().plusDays(20));
        receipt.getBarang().setKategori("Frozen");
        Section matchingSection = section("A", "Frozen");
        Section genericSection = section("B", "Semua");
        Section otherSection = section("C", "Dry");
        Rak matchingRack = new Rak(11, matchingSection, "A-01", 100, 80);
        Rak genericRack = new Rak(12, genericSection, "B-01", 100, 0);
        Rak otherRack = new Rak(13, otherSection, "C-01", 100, 0);
        Rak fullRack = new Rak(14, matchingSection, "A-02", 100, 96);
        when(receiptRepository.findById(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findAll()).thenReturn(
                java.util.List.of(genericRack, otherRack, fullRack, matchingRack));
        when(balanceRepository.findAllForLocationReport()).thenReturn(java.util.List.of());

        var recommendations = service.recommendPutawayLocations(8);

        assertEquals(java.util.List.of(matchingRack, genericRack, otherRack),
                recommendations.stream().map(InboundReceiptService.PutawayRecommendation::rack).toList());
        assertEquals(20, recommendations.get(0).availableCapacity());
        assertTrue(recommendations.get(0).categoryMatch());
        assertFalse(recommendations.get(1).categoryMatch());
        assertFalse(recommendations.get(2).categoryMatch());
    }

    @Test
    void putawayRecommendationsAreUnavailableBeforeQcOrWhenCapacityIsInsufficient() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.QC_PENDING,
                LocalDate.now().plusDays(20));
        when(receiptRepository.findById(8)).thenReturn(Optional.of(receipt));

        assertTrue(service.recommendPutawayLocations(8).isEmpty());

        receipt.setStatus(InboundReceiptStatus.PUTAWAY_PENDING);
        Rak insufficientRack = new Rak(11, section("A", "Semua"), "A-01", 10, 6);
        when(rakRepository.findAll()).thenReturn(java.util.List.of(insufficientRack));
        when(balanceRepository.findAllForLocationReport()).thenReturn(java.util.List.of());
        assertTrue(service.recommendPutawayLocations(8).isEmpty());
    }

    @Test
    void putawayRecommendationsRespectCbmCapacityAndRankByCbmUtilization() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING,
                LocalDate.now().plusDays(20), 1);
        receipt.getBarang().setVolumeCbm(new BigDecimal("0.100000"));
        Section section = section("A", "Semua");
        Rak tooSmall = new Rak(21, section, "A-01", 100, 0);
        tooSmall.setCapacityCbm(new BigDecimal("0.45"));
        Rak nearlyFull = new Rak(22, section, "A-02", 100, 10);
        nearlyFull.setCapacityCbm(new BigDecimal("1.00"));
        Rak betterFit = new Rak(23, section, "A-03", 100, 90);
        betterFit.setCapacityCbm(new BigDecimal("2.00"));
        Barang storedA = Barang.builder().id(2).kodeBarang("BRG-2").namaBarang("Stored A").build();
        storedA.setVolumeCbm(new BigDecimal("0.100000"));
        Barang storedB = Barang.builder().id(3).kodeBarang("BRG-3").namaBarang("Stored B").build();
        storedB.setVolumeCbm(new BigDecimal("0.010000"));
        when(receiptRepository.findById(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findAll()).thenReturn(java.util.List.of(tooSmall, nearlyFull, betterFit));
        when(balanceRepository.findAllForLocationReport()).thenReturn(java.util.List.of(
                new InventoryBalance(storedA, tooSmall, "RAK-21", 4),
                new InventoryBalance(storedA, nearlyFull, "RAK-22", 9),
                new InventoryBalance(storedB, betterFit, "RAK-23", 50)));

        var recommendations = service.recommendPutawayLocations(8);

        assertEquals(java.util.List.of(betterFit, nearlyFull), recommendations.stream()
                .map(InboundReceiptService.PutawayRecommendation::rack).toList());
        assertEquals(new BigDecimal("1.500000"), recommendations.getFirst().availableCbm());
        assertEquals(new BigDecimal("0.100000"), recommendations.getLast().availableCbm());
    }

    @Test
    void putawayRulesFilterIncompatibleStorageTypesAndPreferExactTypeThenPriority() {
        InboundReceipt receipt = receipt(InboundReceiptStatus.PUTAWAY_PENDING,
                LocalDate.now().plusDays(20));
        receipt.getBarang().setStorageType("cold");
        Rak generalRack = new Rak(31, section("A", "Frozen"), "A-01", 100, 0);
        generalRack.getSection().setStorageType("GENERAL");
        generalRack.getSection().setPutawayPriority(0);
        Rak coldRack = new Rak(32, section("B", "Semua"), "B-01", 100, 0);
        coldRack.getSection().setZoneCode("COLD-ZONE");
        coldRack.getSection().setStorageType("COLD");
        coldRack.getSection().setPutawayPriority(90);
        Rak chemicalRack = new Rak(33, section("C", "Frozen"), "C-01", 100, 0);
        chemicalRack.getSection().setStorageType("HAZARDOUS");
        when(receiptRepository.findById(8)).thenReturn(Optional.of(receipt));
        when(rakRepository.findAll()).thenReturn(java.util.List.of(generalRack, chemicalRack, coldRack));
        when(balanceRepository.findAllForLocationReport()).thenReturn(java.util.List.of());

        var recommendations = service.recommendPutawayLocations(8);

        assertEquals(java.util.List.of(coldRack, generalRack), recommendations.stream()
                .map(InboundReceiptService.PutawayRecommendation::rack).toList());
        assertTrue(recommendations.getFirst().storageTypeMatch());
        assertTrue(recommendations.getFirst().getPreferenceLabel().contains("Tipe storage cocok"));
    }

    @Test
    void qrLabelIsReadableAndContainsItemBatchAndExpiry() throws Exception {
        InboundReceipt receipt = receipt(InboundReceiptStatus.QC_PENDING, LocalDate.of(2027, 4, 5));
        when(receiptRepository.findById(8)).thenReturn(Optional.of(receipt));

        Path imagePath = service.generateQrLabel(8, tempDirectory);
        var image = ImageIO.read(imagePath.toFile());
        String payload = new MultiFormatReader().decode(new BinaryBitmap(
            new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
        String[] fields = payload.split("\\|");

        assertTrue(Files.isRegularFile(imagePath));
        assertEquals("WMS1", fields[0]);
        assertEquals("BRG-1", new String(java.util.Base64.getUrlDecoder().decode(fields[1]),
            java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("LOT-01", new String(java.util.Base64.getUrlDecoder().decode(fields[2]),
            java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("2027-04-05", fields[3]);
    }

    @Test
    void returnItemQrCanBeReusedOrRegeneratedWithTheSameInventoryPayload() throws Exception {
        Path first = service.generateItemQrLabel("BRG-1", "LOT-01", null, tempDirectory);
        Path second = service.generateItemQrLabel("BRG-1", "LOT-01", null, tempDirectory);
        var image = ImageIO.read(first.toFile());
        String payload = new MultiFormatReader().decode(new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();

        assertEquals(first, second);
        assertTrue(Files.isRegularFile(first));
        assertEquals(new BarcodePayload.Item("BRG-1", "LOT-01", null),
                BarcodePayload.parseItem(payload));
        assertThrows(java.io.IOException.class,
                () -> service.generateItemQrLabel(" ", "LOT-01", null, tempDirectory));
    }

    private InboundReceipt receipt(InboundReceiptStatus status, LocalDate expiryDate) {
        return receipt(status, expiryDate, 5);
    }

    private InboundReceipt receipt(InboundReceiptStatus status, LocalDate expiryDate, int quantity) {
        Barang barang = Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build();
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User creator = User.builder().id(3).fullName("Receiver").build();
        InboundReceipt receipt = new InboundReceipt("RCV-TEST", barang, supplier, quantity,
                "LOT-01", expiryDate, null, creator, java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(receipt, "id", 8);
        receipt.setStatus(status);
        return receipt;
    }

    private PurchaseOrderLine purchaseOrderLine(int lineId, int ordered, int received) {
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User creator = User.builder().id(3).fullName("Buyer").build();
        PurchaseOrder order = new PurchaseOrder("PO-TEST", supplier, creator, null,
                java.time.LocalDateTime.now());
        order.setStatus(PurchaseOrderStatus.APPROVED);
        ReflectionTestUtils.setField(order, "id", 20);
        PurchaseOrderLine line = new PurchaseOrderLine(
                Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build(), ordered);
        line.setReceivedQuantity(received);
        order.addLine(line);
        ReflectionTestUtils.setField(line, "id", lineId);
        return line;
    }

    private Section section(String code, String targetCategory) {
        Section section = new Section(Integer.valueOf(code.charAt(0)), code, "Section " + code, 12);
        section.setTargetKategori(targetCategory);
        return section;
    }

    private String itemPayload(String sku, String batch, LocalDate expiryDate) {
        var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        return "WMS1|" + encoder.encodeToString(sku.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "|" + encoder.encodeToString(batch.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "|" + expiryDate;
    }

    private String locationPayload(String rackCode) {
        return "SLOC1|" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rackCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
