package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "return_request_line")
public class ReturnRequestLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_line_id")
    private PurchaseOrderLine purchaseOrderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_line_id")
    private DeliveryOrderLine deliveryOrderLine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rak_id", nullable = false)
    private Rak rak;

    @Column(name = "batch_code", nullable = false, length = 64)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Integer quantity;

    protected ReturnRequestLine() {}

    public ReturnRequestLine(PurchaseOrderLine source, Rak rak, String batchCode, Integer quantity) {
        this.purchaseOrderLine = source;
        this.barang = source.getBarang();
        this.rak = rak;
        this.batchCode = batchCode;
        this.quantity = quantity;
    }

    public ReturnRequestLine(DeliveryOrderLine source, Rak rak, String batchCode,
                             LocalDate expiryDate, Integer quantity) {
        this.deliveryOrderLine = source;
        this.barang = source.getBarang();
        this.rak = rak;
        this.batchCode = batchCode;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public ReturnRequest getReturnRequest() { return returnRequest; }
    public PurchaseOrderLine getPurchaseOrderLine() { return purchaseOrderLine; }
    public DeliveryOrderLine getDeliveryOrderLine() { return deliveryOrderLine; }
    public Barang getBarang() { return barang; }
    public Rak getRak() { return rak; }
    public String getBatchCode() { return batchCode; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Integer getQuantity() { return quantity; }
    public void setReturnRequest(ReturnRequest returnRequest) { this.returnRequest = returnRequest; }
}
