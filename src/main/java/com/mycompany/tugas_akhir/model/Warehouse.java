package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "warehouse_code", nullable = false, unique = true, length = 20)
    private String warehouseCode;

    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
    private List<Section> sections = new ArrayList<>();

    protected Warehouse() {}

    public Warehouse(String warehouseCode, String warehouseName, String address) {
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.address = address;
        this.active = true;
    }

    public Integer getId() { return id; }
    public String getWarehouseCode() { return warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public String getAddress() { return address; }
    public Boolean getActive() { return active; }
    public List<Section> getSections() { return sections; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() { return warehouseCode + " - " + warehouseName; }
}
