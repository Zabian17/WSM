package com.mycompany.tugas_akhir.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BarcodePayloadTest {
    @AfterEach
    void restoreDefaultPrefixes() {
        BarcodePayload.configurePrefixes("WMS1", "SLOC1");
    }

    @Test
    void customPrefixesGenerateAndParseItemsAndLocationsWhileAcceptingLegacyCodes() {
        BarcodePayload.configurePrefixes("ITEM-X", "RACK-X");

        String itemPayload = BarcodePayload.itemPayload("SKU-1", "LOT-1", LocalDate.of(2027, 5, 1));
        String locationPayload = BarcodePayload.locationPayload("A-01");

        assertEquals("ITEM-X", itemPayload.substring(0, itemPayload.indexOf('|')));
        assertEquals(new BarcodePayload.Item("SKU-1", "LOT-1", LocalDate.of(2027, 5, 1)),
                BarcodePayload.parseItem(itemPayload));
        assertEquals("A-01", BarcodePayload.parseLocation(locationPayload));
        assertEquals(new BarcodePayload.Item("SKU-1", "LOT-1", null),
                BarcodePayload.parseItem("WMS1|U0tVLTE|TE9ULTE|-"));
        assertEquals("A-01", BarcodePayload.parseLocation("SLOC1|QS0wMQ"));
    }

    @Test
    void rejectsInvalidPrefixesAndDoesNotChangeCurrentConfiguration() {
        BarcodePayload.configurePrefixes("ITEM-X", "RACK-X");

        assertThrows(IllegalArgumentException.class,
                () -> BarcodePayload.configurePrefixes("ITEM|X", "RACK-X"));
        assertEquals("ITEM-X", BarcodePayload.itemPayload("SKU-1", "LOT-1", null)
                .substring(0, "ITEM-X".length()));
        assertNull(BarcodePayload.parseLocation("RACK-X|bad+encoding"));
    }
}
