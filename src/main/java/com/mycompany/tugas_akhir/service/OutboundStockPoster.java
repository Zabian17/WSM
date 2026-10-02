package com.mycompany.tugas_akhir.service;

public interface OutboundStockPoster {

    boolean postOutbound(Integer barangId, Integer rackId, String batchCode,
                         int quantity, String notes, Integer userId);
}