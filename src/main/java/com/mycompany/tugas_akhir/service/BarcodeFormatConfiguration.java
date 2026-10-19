package com.mycompany.tugas_akhir.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BarcodeFormatConfiguration {

    public BarcodeFormatConfiguration(
            @Value("${gudangku.barcode.item-prefix:WMS1}") String itemPrefix,
            @Value("${gudangku.barcode.location-prefix:SLOC1}") String locationPrefix) {
        BarcodePayload.configurePrefixes(itemPrefix, locationPrefix);
    }
}
