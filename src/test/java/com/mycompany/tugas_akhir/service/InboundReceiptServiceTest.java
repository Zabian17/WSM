package com.mycompany.tugas_akhir.service;

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
    @Mock private BarangRepository barangRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private RakRepository rakRepository;
    @Mock private UserRepository userRepository;
    @Mock private InboundStockPoster stockPoster;

    @TempDir Path tempDirectory;

    private InboundReceiptService service;

    @BeforeEach
    void setUp() {
        service = new InboundReceiptService(receiptRepository, barangRepository, supplierRepository,
            rakRepository, userRepository, stockPoster);
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

    private InboundReceipt receipt(InboundReceiptStatus status, LocalDate expiryDate) {
        Barang barang = Barang.builder().id(1).kodeBarang("BRG-1").namaBarang("Item").build();
        Supplier supplier = new Supplier(2, "Vendor", null, null, null);
        User creator = User.builder().id(3).fullName("Receiver").build();
        InboundReceipt receipt = new InboundReceipt("RCV-TEST", barang, supplier, 5,
                "LOT-01", expiryDate, null, creator, java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(receipt, "id", 8);
        receipt.setStatus(status);
        return receipt;
    }
}