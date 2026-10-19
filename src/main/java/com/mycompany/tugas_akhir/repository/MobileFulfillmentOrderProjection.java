package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.DeliveryOrderStatus;

import java.time.LocalDateTime;

public record MobileFulfillmentOrderProjection(Integer id, String orderCode, String customerName,
                                               DeliveryOrderStatus status, LocalDateTime createdAt) {}
