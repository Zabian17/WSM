ALTER TABLE barang
    ADD COLUMN volume_cbm DECIMAL(12,6) NULL AFTER satuan;

ALTER TABLE rak
    ADD COLUMN capacity_cbm DECIMAL(12,6) NULL AFTER terisi;
