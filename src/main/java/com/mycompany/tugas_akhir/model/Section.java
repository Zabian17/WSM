package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "kode_section", nullable = false, unique = true, length = 10)
    private String kodeSection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "nama_section", length = 80)
    private String namaSection;

    @Column(columnDefinition = "int default 12")
    private Integer kapasitas = 12;

    @Column(name = "target_kategori", length = 80)
    private String targetKategori = "Semua";

    @Column(name = "zone_code", length = 40)
    private String zoneCode;

    @Column(name = "storage_type", nullable = false, length = 40)
    private String storageType = "GENERAL";

    @Column(name = "putaway_priority", nullable = false)
    private Integer putawayPriority = 50;

    @Column(name = "alert_threshold")
    private Integer alertThreshold = 80;

    @Column(name = "alert_type", length = 50)
    private String alertType = "Kapasitas Penuh (>= %)";

    @Column(name = "alert_active")
    private Boolean alertActive = true;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Rak> raks = new ArrayList<>();

    public Section() {}

    public Section(Integer id, String kodeSection, String namaSection, Integer kapasitas) {
        this.id = id;
        this.kodeSection = kodeSection;
        this.namaSection = namaSection;
        this.kapasitas = kapasitas != null ? kapasitas : 12;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getKodeSection() { return kodeSection; }
    public void setKodeSection(String kodeSection) { this.kodeSection = kodeSection; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public String getNamaSection() { return namaSection; }
    public void setNamaSection(String namaSection) { this.namaSection = namaSection; }

    public Integer getKapasitas() { return kapasitas != null ? kapasitas : 12; }
    public void setKapasitas(Integer kapasitas) { this.kapasitas = kapasitas; }

    public String getTargetKategori() { return targetKategori != null ? targetKategori : "Semua"; }
    public void setTargetKategori(String targetKategori) { this.targetKategori = targetKategori; }
    public String getZoneCode() { return zoneCode != null ? zoneCode : ""; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getStorageType() { return storageType != null ? storageType : "GENERAL"; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public Integer getPutawayPriority() { return putawayPriority != null ? putawayPriority : 50; }
    public void setPutawayPriority(Integer putawayPriority) { this.putawayPriority = putawayPriority; }

    public Integer getAlertThreshold() { return alertThreshold != null ? alertThreshold : 80; }
    public void setAlertThreshold(Integer alertThreshold) { this.alertThreshold = alertThreshold; }

    public String getAlertType() { return alertType != null ? alertType : "Kapasitas Penuh (>= %)"; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public Boolean getAlertActive() { return alertActive != null ? alertActive : true; }
    public void setAlertActive(Boolean alertActive) { this.alertActive = alertActive; }

    public List<Rak> getRaks() { return raks; }
    public void setRaks(List<Rak> raks) { this.raks = raks; }

    public static SectionBuilder builder() {
        return new SectionBuilder();
    }

    public static class SectionBuilder {
        private Integer id;
        private String kodeSection;
        private String namaSection;
        private Integer kapasitas = 12;
        private String targetKategori = "Semua";
        private Integer alertThreshold = 80;
        private String alertType = "Kapasitas Penuh (>= %)";
        private Boolean alertActive = true;

        public SectionBuilder id(Integer id) { this.id = id; return this; }
        public SectionBuilder kodeSection(String kodeSection) { this.kodeSection = kodeSection; return this; }
        public SectionBuilder namaSection(String namaSection) { this.namaSection = namaSection; return this; }
        public SectionBuilder kapasitas(Integer kapasitas) { this.kapasitas = kapasitas; return this; }
        public SectionBuilder targetKategori(String targetKategori) { this.targetKategori = targetKategori; return this; }
        public SectionBuilder alertThreshold(Integer alertThreshold) { this.alertThreshold = alertThreshold; return this; }
        public SectionBuilder alertType(String alertType) { this.alertType = alertType; return this; }
        public SectionBuilder alertActive(Boolean alertActive) { this.alertActive = alertActive; return this; }

        public Section build() {
            Section s = new Section(id, kodeSection, namaSection, kapasitas);
            s.setTargetKategori(targetKategori);
            s.setAlertThreshold(alertThreshold);
            s.setAlertType(alertType);
            s.setAlertActive(alertActive);
            return s;
        }
    }
}
