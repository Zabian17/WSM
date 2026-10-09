package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Barang;
import com.mycompany.tugas_akhir.model.Customer;
import com.mycompany.tugas_akhir.model.Supplier;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.SupplierRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MasterDataImportService {

    private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 1000;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public enum Dataset {
        ITEM("Barang", List.of("Kode Barang", "Nama Barang", "Kategori", "Satuan", "Stok Minimum")),
        CUSTOMER("Customer", List.of("Nama Customer", "Posisi", "Alamat", "Kontak", "Email", "Status")),
        SUPPLIER("Supplier", List.of("Nama Supplier", "Kontak", "Email", "Alamat"));

        private final String label;
        private final List<String> headers;

        Dataset(String label, List<String> headers) {
            this.label = label;
            this.headers = headers;
        }

        public String getLabel() { return label; }
        public List<String> getHeaders() { return headers; }

        @Override
        public String toString() { return label; }
    }

    public record PreviewRow(int rowNumber, List<String> values, String error) {
        public boolean isValid() { return error == null || error.isBlank(); }
    }

    public record ImportPreview(Dataset dataset, List<String> headers, List<PreviewRow> rows) {
        public boolean isValid() {
            return !rows.isEmpty() && rows.stream().allMatch(PreviewRow::isValid);
        }

        public long getInvalidRowCount() {
            return rows.stream().filter(row -> !row.isValid()).count();
        }
    }

    private final BarangRepository barangRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BarangService barangService;

    public MasterDataImportService(BarangRepository barangRepository,
                                  CustomerRepository customerRepository,
                                  SupplierRepository supplierRepository,
                                  BarangService barangService) {
        this.barangRepository = barangRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.barangService = barangService;
    }

    public byte[] createTemplate(Dataset dataset) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(dataset.getLabel());
            Row header = sheet.createRow(0);
            for (int column = 0; column < dataset.headers.size(); column++) {
                header.createCell(column).setCellValue(dataset.headers.get(column));
                sheet.setColumnWidth(column, 24 * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(Dataset dataset, InputStream source) throws IOException {
        if (dataset == null || source == null) {
            throw new IllegalArgumentException("Jenis data dan file Excel wajib dipilih.");
        }
        byte[] bytes = source.readNBytes(MAX_UPLOAD_BYTES + 1);
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Ukuran file maksimal 5 MB.");
        }

        List<List<String>> parsedRows = readRows(dataset, bytes);
        return validateRows(dataset, parsedRows);
    }

    @Transactional
    public int importRows(Dataset dataset, List<List<String>> rows) {
        if (dataset == null || rows == null || rows.isEmpty() || rows.size() > MAX_DATA_ROWS) {
            throw new IllegalArgumentException("Data impor kosong atau melebihi batas 1000 baris.");
        }
        ImportPreview preview = validateRows(dataset, rows);
        if (!preview.isValid()) {
            throw new IllegalArgumentException("Impor dibatalkan: " + preview.getInvalidRowCount()
                    + " baris tidak valid atau duplikat.");
        }

        for (PreviewRow row : preview.rows()) {
            List<String> values = row.values();
            switch (dataset) {
                case ITEM -> barangService.addBarang(values.get(0).trim(), values.get(1).trim(),
                        emptyToNull(values.get(2)), emptyToNull(values.get(3)), 0,
                        Integer.parseInt(values.get(4).trim()));
                case CUSTOMER -> customerRepository.save(new Customer(null, values.get(0).trim(),
                        emptyToNull(values.get(1)), emptyToNull(values.get(2)), emptyToNull(values.get(3)),
                        emptyToNull(values.get(4)), normalizeStatus(values.get(5))));
                case SUPPLIER -> supplierRepository.save(new Supplier(null, values.get(0).trim(),
                        emptyToNull(values.get(1)), emptyToNull(values.get(2)), emptyToNull(values.get(3))));
            }
        }
        return preview.rows().size();
    }

    private List<List<String>> readRows(Dataset dataset, byte[] bytes) throws IOException {
        try (InputStream input = new ByteArrayInputStream(bytes);
             Workbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Workbook tidak memiliki sheet.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || !readRow(dataset, header, new DataFormatter()).equals(dataset.headers)) {
                throw new IllegalArgumentException("Header Excel tidak sesuai template "
                        + dataset.getLabel() + ".");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<List<String>> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                List<String> values = readRow(dataset, row, formatter);
                if (values.stream().allMatch(String::isBlank)) continue;
                rows.add(values);
                if (rows.size() > MAX_DATA_ROWS) {
                    throw new IllegalArgumentException("File Excel maksimal berisi 1000 baris data.");
                }
            }
            return rows;
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException exception) {
            throw new IllegalArgumentException("Format file harus Excel .xlsx.", exception);
        }
    }

    private List<String> readRow(Dataset dataset, Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>(dataset.headers.size());
        for (int column = 0; column < dataset.headers.size(); column++) {
            Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return values;
    }

    private ImportPreview validateRows(Dataset dataset, List<List<String>> rows) {
        Set<String> existingKeys = loadExistingKeys(dataset);
        Set<String> uploadedKeys = new HashSet<>();
        List<PreviewRow> previewRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<String> values = normalizeWidth(dataset, rows.get(index));
            List<String> errors = validateRow(dataset, values);
            if (errors.isEmpty()) {
                String key = duplicateKey(dataset, values);
                if (!uploadedKeys.add(key)) errors.add("Duplikat di dalam file.");
                else if (existingKeys.contains(key)) errors.add("Data dengan kode/nama ini sudah ada.");
            }
            previewRows.add(new PreviewRow(index + 2, List.copyOf(values), String.join(" ", errors)));
        }
        if (previewRows.isEmpty()) {
            previewRows.add(new PreviewRow(0, List.of(), "File tidak berisi baris data."));
        }
        return new ImportPreview(dataset, dataset.headers, List.copyOf(previewRows));
    }

    private List<String> validateRow(Dataset dataset, List<String> values) {
        List<String> errors = new ArrayList<>();
        switch (dataset) {
            case ITEM -> {
                required(values.get(0), "Kode barang", 20, errors);
                required(values.get(1), "Nama barang", 150, errors);
                maxLength(values.get(2), "Kategori", 80, errors);
                maxLength(values.get(3), "Satuan", 20, errors);
                if (!isNonNegativeInteger(values.get(4))) errors.add("Stok minimum harus bilangan bulat nol atau lebih.");
            }
            case CUSTOMER -> {
                required(values.get(0), "Nama customer", 150, errors);
                maxLength(values.get(1), "Posisi", 100, errors);
                maxLength(values.get(3), "Kontak", 20, errors);
                validateEmail(values.get(4), errors);
                String status = values.get(5).trim();
                if (!status.isEmpty() && !status.equalsIgnoreCase("Aktif")
                        && !status.equalsIgnoreCase("Nonaktif")) {
                    errors.add("Status customer harus Aktif atau Nonaktif.");
                }
            }
            case SUPPLIER -> {
                required(values.get(0), "Nama supplier", 150, errors);
                maxLength(values.get(1), "Kontak", 20, errors);
                validateEmail(values.get(2), errors);
            }
        }
        return errors;
    }

    private Set<String> loadExistingKeys(Dataset dataset) {
        return switch (dataset) {
            case ITEM -> barangRepository.findAll().stream()
                    .map(Barang::getKodeBarang).map(this::normalizeKey).collect(java.util.stream.Collectors.toSet());
            case CUSTOMER -> customerRepository.findAll().stream()
                    .map(Customer::getNamaCustomer).map(this::normalizeKey).collect(java.util.stream.Collectors.toSet());
            case SUPPLIER -> supplierRepository.findAll().stream()
                    .map(Supplier::getNamaSupplier).map(this::normalizeKey).collect(java.util.stream.Collectors.toSet());
        };
    }

    private String duplicateKey(Dataset dataset, List<String> values) {
        return normalizeKey(values.get(dataset == Dataset.ITEM ? 0 : 0));
    }

    private List<String> normalizeWidth(Dataset dataset, List<String> values) {
        List<String> normalized = new ArrayList<>(dataset.headers.size());
        for (int index = 0; index < dataset.headers.size(); index++) {
            normalized.add(values != null && index < values.size() && values.get(index) != null
                    ? values.get(index).trim() : "");
        }
        return normalized;
    }

    private void required(String value, String label, int maxLength, List<String> errors) {
        if (value.isBlank()) errors.add(label + " wajib diisi.");
        maxLength(value, label, maxLength, errors);
    }

    private void maxLength(String value, String label, int maxLength, List<String> errors) {
        if (value.length() > maxLength) errors.add(label + " maksimal " + maxLength + " karakter.");
    }

    private void validateEmail(String email, List<String> errors) {
        String value = email.trim();
        if (!value.isEmpty() && (value.length() > 150 || !EMAIL_PATTERN.matcher(value).matches())) {
            errors.add("Format email tidak valid.");
        }
    }

    private boolean isNonNegativeInteger(String value) {
        try {
            return !value.isBlank() && Integer.parseInt(value.trim()) >= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeStatus(String value) {
        return value == null || value.isBlank() ? "Aktif"
                : value.trim().equalsIgnoreCase("Nonaktif") ? "Nonaktif" : "Aktif";
    }
}
