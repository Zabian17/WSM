package com.mycompany.tugas_akhir.service;

import java.time.LocalDateTime;

public record OperationalReportRow(
        String category,
        String documentCode,
        String status,
        String partner,
        String itemCode,
        int quantity,
        String metric,
        String actor,
        LocalDateTime timestamp) {}
