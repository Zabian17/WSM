package com.mycompany.tugas_akhir.service;

import java.time.LocalDate;

public interface InboundStockPoster {

    boolean postInbound(Integer barangId, Integer supplierId, Integer rakId, String batchCode,
                        LocalDate expiryDate, int quantity, String notes, Integer userId);
}