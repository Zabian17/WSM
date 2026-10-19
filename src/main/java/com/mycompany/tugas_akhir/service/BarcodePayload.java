package com.mycompany.tugas_akhir.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class BarcodePayload {

    public record Item(String sku, String batchCode, LocalDate expiryDate) {}

    private static volatile String itemPrefix = "WMS1";
    private static volatile String locationPrefix = "SLOC1";

    private BarcodePayload() {}

    public static void configurePrefixes(String newItemPrefix, String newLocationPrefix) {
        validatePrefix(newItemPrefix);
        validatePrefix(newLocationPrefix);
        itemPrefix = newItemPrefix;
        locationPrefix = newLocationPrefix;
    }

    public static String itemPayload(String sku, String batchCode, LocalDate expiryDate) {
        return itemPrefix + "|" + encode(sku) + "|" + encode(batchCode)
                + "|" + (expiryDate == null ? "-" : expiryDate);
    }

    public static String locationPayload(String rackCode) {
        return locationPrefix + "|" + encode(rackCode);
    }

    public static Item parseItem(String payload) {
        if (payload == null) return null;
        String[] fields = payload.trim().split("\\|", -1);
        if (fields.length != 4 || !(itemPrefix.equals(fields[0]) || "WMS1".equals(fields[0]))) return null;
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
        if (fields.length != 2 || !(locationPrefix.equals(fields[0]) || "SLOC1".equals(fields[0]))) return null;
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

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void validatePrefix(String prefix) {
        if (prefix == null || !prefix.matches("[A-Za-z0-9_-]{1,16}")) {
            throw new IllegalArgumentException("Barcode prefix must contain 1-16 letters, digits, underscores, or hyphens.");
        }
    }
}
