package com.mycompany.tugas_akhir.repository;

public record StockCountReviewLineProjection(Integer id, String itemCode, String itemName,
                                             String unit, Integer expectedQuantity,
                                             Integer countedQuantity) {}
