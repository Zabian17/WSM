package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "material_usage_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_usage_item",
                columnNames = {"material_usage_id", "barang_id"}))
public class MaterialUsageLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_usage_id", nullable = false)
    private MaterialUsage materialUsage;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    protected MaterialUsageLine() {}

    public MaterialUsageLine(Barang barang, Integer requestedQuantity) {
        this.barang = barang;
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getId() { return id; }
    public MaterialUsage getMaterialUsage() { return materialUsage; }
    public Barang getBarang() { return barang; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setMaterialUsage(MaterialUsage materialUsage) { this.materialUsage = materialUsage; }
}
