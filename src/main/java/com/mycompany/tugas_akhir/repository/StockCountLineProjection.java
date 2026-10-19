package com.mycompany.tugas_akhir.repository;

public record StockCountLineProjection(Integer id, String itemCode, String itemName,
                                       String unit, String assignedTo) {}
