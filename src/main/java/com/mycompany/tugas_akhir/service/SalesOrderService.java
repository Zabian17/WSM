package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderRepository;
import com.mycompany.tugas_akhir.repository.SalesOrderRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final CustomerRepository customerRepository;
    private final BarangRepository barangRepository;
    private final UserRepository userRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             DeliveryOrderRepository deliveryOrderRepository,
                             CustomerRepository customerRepository,
                             BarangRepository barangRepository,
                             UserRepository userRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.customerRepository = customerRepository;
        this.barangRepository = barangRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> getRecentSalesOrders() {
        return salesOrderRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional
    public SalesOrder createSalesOrder(Integer customerId, Integer userId,
                                       Map<Integer, Integer> requestedQuantities, String notes) {
        if (customerId == null || userId == null || requestedQuantities == null
                || requestedQuantities.isEmpty() || requestedQuantities.size() > 100) {
            return null;
        }
        Customer customer = customerRepository.findById(customerId).orElse(null);
        User creator = userRepository.findById(userId).orElse(null);
        if (customer == null || creator == null || "Nonaktif".equalsIgnoreCase(customer.getStatus())) return null;
        SalesOrder order = new SalesOrder("SO-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                customer, creator, notes, LocalDateTime.now());
        for (Map.Entry<Integer, Integer> request : requestedQuantities.entrySet()) {
            if (request.getKey() == null || request.getValue() == null || request.getValue() <= 0) return null;
            Barang item = barangRepository.findById(request.getKey()).orElse(null);
            if (item == null) return null;
            order.addLine(new SalesOrderLine(item, request.getValue()));
        }
        return salesOrderRepository.save(order);
    }

    @Transactional
    public DeliveryOrder convertToDeliveryOrder(Integer salesOrderId, Integer userId) {
        if (salesOrderId == null || userId == null) return null;
        SalesOrder order = salesOrderRepository.findByIdWithLinesForUpdate(salesOrderId).orElse(null);
        User creator = userRepository.findById(userId).orElse(null);
        if (order == null || creator == null || order.getStatus() == SalesOrderStatus.CONVERTED) return null;
        List<SalesOrderLine> remainingLines = order.getLines().stream()
                .filter(line -> line.getRemainingQuantity() > 0).toList();
        if (remainingLines.isEmpty()) return null;

        DeliveryOrder deliveryOrder = new DeliveryOrder("DO-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT),
                order.getCustomer(), order, creator, "Dibuat dari " + order.getOrderCode(), LocalDateTime.now());
        for (SalesOrderLine line : remainingLines) {
            int remaining = line.getRemainingQuantity();
            deliveryOrder.addLine(new DeliveryOrderLine(line.getBarang(), line, remaining));
            line.setConvertedQuantity(Math.addExact(line.getConvertedQuantity(), remaining));
        }
        order.setStatus(order.getLines().stream().anyMatch(line -> line.getRemainingQuantity() > 0)
                ? SalesOrderStatus.PARTIALLY_CONVERTED : SalesOrderStatus.CONVERTED);
        return deliveryOrderRepository.save(deliveryOrder);
    }
}
