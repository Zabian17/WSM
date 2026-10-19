package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.Transaksi;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.type.OrientationEnum;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportPdfService {

    private static final String[] FIELDS = {
            "code", "type", "item", "supplier", "quantity", "sloc", "staff", "date", "notes"
    };
    private static final String[] HEADERS = {
            "Kode", "Tipe", "Barang", "Supplier", "Jumlah", "SLOC", "Petugas", "Tanggal", "Keterangan"
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportTransactions(List<Transaksi> transactions, Path destination) throws Exception {
        List<Map<String, ?>> rows = new ArrayList<>(transactions.size());
        for (Transaksi transaction : transactions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", value(transaction.getKodeTransaksi()));
            row.put("type", "masuk".equalsIgnoreCase(transaction.getTipe()) ? "Masuk" : "Keluar");
            row.put("item", transaction.getBarang() == null ? "-" : value(transaction.getBarang().getNamaBarang()));
            row.put("supplier", transaction.getSupplier() == null ? "-" : value(transaction.getSupplier().getNamaSupplier()));
            row.put("quantity", String.valueOf(transaction.getJumlah()));
            row.put("sloc", transaction.getRak() == null ? "-" : value(transaction.getRak().getKodeRak()));
            row.put("staff", transaction.getUser() == null ? "System" : value(transaction.getUser().getFullName()));
            row.put("date", transaction.getTanggal() == null ? "-" : transaction.getTanggal().format(DATE_FORMAT));
            row.put("notes", value(transaction.getKeterangan()));
            rows.add(row);
        }

        JasperPrint report = JasperFillManager.fillReport(JasperCompileManager.compileReport(createDesign()),
                Map.of("reportTitle", "Laporan Transaksi Gudang"), new JRMapCollectionDataSource(rows));
        JasperExportManager.exportReportToPdfFile(report, destination.toAbsolutePath().toString());
    }

    private JasperDesign createDesign() throws Exception {
        JasperDesign design = new JasperDesign();
        design.setName("GudangKuTransactionReport");
        design.setPageWidth(842);
        design.setPageHeight(595);
        design.setOrientation(OrientationEnum.LANDSCAPE);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(24);
        design.setBottomMargin(20);
        design.setColumnWidth(802);

        JRDesignField titleField = new JRDesignField();
        titleField.setName("reportTitle");
        titleField.setValueClass(String.class);
        design.addField(titleField);
        for (String name : FIELDS) {
            JRDesignField field = new JRDesignField();
            field.setName(name);
            field.setValueClass(String.class);
            design.addField(field);
        }

        JRDesignBand title = new JRDesignBand();
        title.setHeight(32);
        JRDesignTextField titleText = new JRDesignTextField();
        titleText.setX(0);
        titleText.setY(0);
        titleText.setWidth(802);
        titleText.setHeight(24);
        titleText.setFontSize(16f);
        titleText.setBold(true);
        titleText.setExpression(new JRDesignExpression("$F{reportTitle}"));
        title.addElement(titleText);
        design.setTitle(title);

        JRDesignBand header = new JRDesignBand();
        header.setHeight(26);
        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(24);
        int x = 0;
        for (int i = 0; i < FIELDS.length; i++) {
            int width = i == FIELDS.length - 1 ? 802 - x : 89;
            JRDesignStaticText headerCell = new JRDesignStaticText();
            headerCell.setX(x);
            headerCell.setY(0);
            headerCell.setWidth(width);
            headerCell.setHeight(26);
            headerCell.setFontSize(8f);
            headerCell.setBold(true);
            headerCell.setText(HEADERS[i]);
            header.addElement(headerCell);

            JRDesignTextField valueCell = new JRDesignTextField();
            valueCell.setX(x);
            valueCell.setY(0);
            valueCell.setWidth(width);
            valueCell.setHeight(24);
            valueCell.setFontSize(7f);
            valueCell.setStretchWithOverflow(true);
            valueCell.setExpression(new JRDesignExpression("$F{" + FIELDS[i] + "}"));
            detail.addElement(valueCell);
            x += width;
        }
        design.setColumnHeader(header);
        ((JRDesignSection) design.getDetailSection()).addBand(detail);
        return design;
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }
}
