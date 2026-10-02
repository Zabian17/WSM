package com.mycompany.tugas_akhir.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rak")
public class Rak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "kode_rak", nullable = false, unique = true, length = 10)
    private String kodeRak;

    @Column(columnDefinition = "int default 100")
    private Integer kapasitas = 100;

    @Column(columnDefinition = "int default 0")
    private Integer terisi = 0;

    public Rak() {}

    public Rak(Integer id, Section section, String kodeRak, Integer kapasitas, Integer terisi) {
        this.id = id;
        this.section = section;
        this.kodeRak = kodeRak;
        this.kapasitas = kapasitas != null ? kapasitas : 100;
        this.terisi = terisi != null ? terisi : 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public String getKodeRak() { return kodeRak; }
    public void setKodeRak(String kodeRak) { this.kodeRak = kodeRak; }

    public Integer getKapasitas() { return kapasitas != null ? kapasitas : 100; }
    public void setKapasitas(Integer kapasitas) { this.kapasitas = kapasitas; }

    public Integer getTerisi() { return terisi != null ? terisi : 0; }
    public void setTerisi(Integer terisi) { this.terisi = terisi; }

    public int getPersentase() {
        if (kapasitas == null || kapasitas <= 0) return 0;
        return (int) Math.round(((double) getTerisi() / getKapasitas()) * 100.0);
    }
}
