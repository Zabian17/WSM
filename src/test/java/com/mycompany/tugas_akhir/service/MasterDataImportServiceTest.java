package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.InventoryBalanceRepository;
import com.mycompany.tugas_akhir.repository.InventoryLotBalanceRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import com.mycompany.tugas_akhir.service.MasterDataImportService.Dataset;
import com.mycompany.tugas_akhir.service.MasterDataImportService.ImportPreview;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterDataImportServiceTest {

    @Mock private BarangRepository barangRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock private InventoryLotBalanceRepository inventoryLotBalanceRepository;

    private MasterDataImportService service;
    private BarangService barangService;

    @BeforeEach
    void setUp() {
        barangService = new BarangService(barangRepository, inventoryBalanceRepository, inventoryLotBalanceRepository);
        service = new MasterDataImportService(barangRepository, customerRepository, supplierRepository, barangService);
    }

    @Test
    void createsWorkbookTemplateWithExpectedHeaders() throws Exception {
        byte[] template = service.createTemplate(Dataset.ITEM);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            assertEquals("Kode Barang", header.getCell(0).getStringCellValue());
            assertEquals("Stok Minimum", header.getCell(4).getStringCellValue());
        }
    }

    @Test
    void previewMarksDuplicateRowsAndRejectsExistingCustomerNames() throws Exception {
        when(customerRepository.findAll()).thenReturn(List.of(
                new Customer(1, "Customer Lama", null, null, null, null, "Aktif")));
        byte[] workbook = workbook(Dataset.CUSTOMER, List.of(
                List.of("Customer Baru", "", "", "", "", ""),
                List.of("customer baru", "", "", "", "", ""),
                List.of("Customer Lama", "", "", "", "", "")));

        ImportPreview preview = service.preview(Dataset.CUSTOMER, new ByteArrayInputStream(workbook));

        assertFalse(preview.isValid());
        assertEquals(2, preview.getInvalidRowCount());
        assertTrue(preview.rows().get(1).error().contains("Duplikat"));
        assertTrue(preview.rows().get(2).error().contains("sudah ada"));
    }

    @Test
    void invalidFileDoesNotSaveAnyRows() {
        when(customerRepository.findAll()).thenReturn(List.of());
        List<List<String>> rows = List.of(
                List.of("Customer Baru", "", "", "", "", "Aktif"),
                List.of("customer baru", "", "", "", "", "Aktif"));

        assertThrows(IllegalArgumentException.class, () -> service.importRows(Dataset.CUSTOMER, rows));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void importsValidCustomerAndDefaultsBlankStatusToActive() {
        when(customerRepository.findAll()).thenReturn(List.of());
        List<List<String>> rows = List.of(List.of("Customer Baru", "", "", "", "", ""));

        assertEquals(1, service.importRows(Dataset.CUSTOMER, rows));

        verify(customerRepository).save(argThat(customer ->
                "Customer Baru".equals(customer.getNamaCustomer()) && "Aktif".equals(customer.getStatus())));
    }

    @Test
    void importsItemWithoutBypassingInventoryInitialization() {
        when(barangRepository.findAll()).thenReturn(List.of());
        when(barangRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, service.importRows(Dataset.ITEM,
                List.of(List.of("BRG-100", "Barang Baru", "", "", "5"))));

        verify(barangRepository).save(argThat(item ->
                "BRG-100".equals(item.getKodeBarang()) && item.getStok() == 0 && item.getStokMin() == 5));
        verify(inventoryBalanceRepository).save(any());
        verify(inventoryLotBalanceRepository).save(any());
    }

    @Test
    void rejectsWorkbookWithMismatchedHeaders() throws Exception {
        byte[] workbook = workbook(Dataset.SUPPLIER, List.of(List.of("Vendor", "", "", "")));

        assertThrows(IllegalArgumentException.class, () ->
                service.preview(Dataset.CUSTOMER, new ByteArrayInputStream(workbook)));
    }

    private byte[] workbook(Dataset dataset, List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            for (int column = 0; column < dataset.getHeaders().size(); column++) {
                header.createCell(column).setCellValue(dataset.getHeaders().get(column));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < rows.get(rowIndex).size(); column++) {
                    row.createCell(column).setCellValue(rows.get(rowIndex).get(column));
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
