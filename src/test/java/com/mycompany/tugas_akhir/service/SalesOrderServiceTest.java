package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.*;
import com.mycompany.tugas_akhir.repository.BarangRepository;
import com.mycompany.tugas_akhir.repository.CustomerRepository;
import com.mycompany.tugas_akhir.repository.DeliveryOrderRepository;
import com.mycompany.tugas_akhir.repository.SalesOrderRepository;
import com.mycompany.tugas_akhir.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BarangRepository barangRepository;
    @Mock private UserRepository userRepository;

    private SalesOrderService service;

    @BeforeEach
    void setUp() {
        service = new SalesOrderService(salesOrderRepository, deliveryOrderRepository,
                customerRepository, barangRepository, userRepository);
    }

    @Test
    void createsOpenSalesOrderWithRequestedLines() {
        Customer customer = customer();
        User creator = user(3);
        Barang item = item();
        when(customerRepository.findById(2)).thenReturn(Optional.of(customer));
        when(userRepository.findById(3)).thenReturn(Optional.of(creator));
        when(barangRepository.findById(1)).thenReturn(Optional.of(item));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesOrder order = service.createSalesOrder(2, 3, Map.of(1, 8), "prioritas");

        assertNotNull(order);
        assertTrue(order.getOrderCode().startsWith("SO-"));
        assertEquals(SalesOrderStatus.OPEN, order.getStatus());
        assertEquals(8, order.getLines().get(0).getRemainingQuantity());
    }

    @Test
    void convertsRemainingSalesOrderQuantityIntoLinkedDeliveryOrder() {
        SalesOrder order = salesOrder();
        order.getLines().get(0).setConvertedQuantity(4);
        User creator = user(9);
        when(salesOrderRepository.findByIdWithLinesForUpdate(20)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(creator));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrder deliveryOrder = service.convertToDeliveryOrder(20, 9);

        assertNotNull(deliveryOrder);
        assertEquals(order, deliveryOrder.getSalesOrder());
        assertEquals(order.getCustomer(), deliveryOrder.getCustomer());
        assertEquals(6, deliveryOrder.getLines().get(0).getOrderedQuantity());
        assertEquals(order.getLines().get(0), deliveryOrder.getLines().get(0).getSalesOrderLine());
        assertEquals(SalesOrderStatus.CONVERTED, order.getStatus());
    }

    @Test
    void doesNotCreateAnotherDeliveryOrderAfterSalesOrderIsConverted() {
        SalesOrder order = salesOrder();
        order.setStatus(SalesOrderStatus.CONVERTED);
        when(salesOrderRepository.findByIdWithLinesForUpdate(20)).thenReturn(Optional.of(order));
        when(userRepository.findById(9)).thenReturn(Optional.of(user(9)));

        assertNull(service.convertToDeliveryOrder(20, 9));
        verifyNoInteractions(deliveryOrderRepository);
    }

    private SalesOrder salesOrder() {
        SalesOrder order = new SalesOrder("SO-TEST", customer(), user(3), null, LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 20);
        order.addLine(new SalesOrderLine(item(), 10));
        return order;
    }

    private Customer customer() {
        return new Customer(2, "Customer", null, null, null, null, "Aktif");
    }

    private User user(int id) {
        return User.builder().id(id).fullName("User " + id).build();
    }

    private Barang item() {
        return Barang.builder().id(1).kodeBarang("SKU-1").namaBarang("Item").build();
    }
}
