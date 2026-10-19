package com.mycompany.tugas_akhir.api.mobile;

import com.mycompany.tugas_akhir.model.StockCountCycleType;
import com.mycompany.tugas_akhir.model.StockCountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MobileStockCountSummary(Integer id, String countCode, String cycleCode,
                                      StockCountCycleType cycleType, String rackCode,
                                      StockCountStatus status, LocalDate dueDate,
                                      LocalDateTime createdAt) {}
