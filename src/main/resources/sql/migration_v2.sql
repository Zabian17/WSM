











USE gudang_akhir;


ALTER TABLE barang 
ADD COLUMN stok_max INT DEFAULT 0 AFTER stok_min;


UPDATE barang 
SET stok_max = stok_min * 2 
WHERE stok_max = 0 AND stok_min > 0;


UPDATE barang 
SET updated_at = CURRENT_TIMESTAMP 
WHERE updated_at IS NULL;


CREATE INDEX idx_barang_stok ON barang(stok, stok_min);























