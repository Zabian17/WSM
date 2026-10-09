package com.mycompany.tugas_akhir.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class BarcodePayload {

    public record Item(String sku, String batchCode, LocalDate expiryDate) {}

    private BarcodePayload() {}

    public static Item parseItem(String payload) {
        if (payload == null) return null;
        String[] fields = payload.trim().split("\\|", -1);
        if (fields.length != 4 || !"WMS1".equals(fields[0])) return null;
        String sku = decode(fields[1]);
        String batchCode = decode(fields[2]);
        if (sku == null || sku.isBlank() || batchCode == null || batchCode.isBlank()) return null;
        LocalDate expiryDate = null;
        if (!"-".equals(fields[3])) {
            try {
                expiryDate = LocalDate.parse(fields[3]);
            } catch (DateTimeParseException exception) {
                return null;
            }
        }
        return new Item(sku, batchCode, expiryDate);
    }

    public static String parseLocation(String payload) {
        if (payload == null) return null;
        String[] fields = payload.trim().split("\\|", -1);
        if (fields.length != 2 || !"SLOC1".equals(fields[0])) return null;
        String rackCode = decode(fields[1]);
        return rackCode != null && rackCode.matches("[A-Za-z0-9._-]{1,10}") ? rackCode : null;
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
