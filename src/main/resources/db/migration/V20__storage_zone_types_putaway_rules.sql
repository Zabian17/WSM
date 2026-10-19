SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'barang' AND column_name = 'storage_type'),
    'SELECT 1',
    'ALTER TABLE barang ADD COLUMN storage_type VARCHAR(40) NULL'
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'target_kategori'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN target_kategori VARCHAR(80) NOT NULL DEFAULT ''Semua'''
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'alert_threshold'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN alert_threshold INT NOT NULL DEFAULT 80'
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'alert_type'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN alert_type VARCHAR(50) NOT NULL DEFAULT ''Kapasitas Penuh (>= %)'''
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'alert_active'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN alert_active BOOLEAN NOT NULL DEFAULT TRUE'
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'zone_code'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN zone_code VARCHAR(40) NULL'
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'storage_type'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN storage_type VARCHAR(40) NOT NULL DEFAULT ''GENERAL'''
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'section' AND column_name = 'putaway_priority'),
    'SELECT 1',
    'ALTER TABLE section ADD COLUMN putaway_priority INT NOT NULL DEFAULT 50'
);
PREPARE migration_statement FROM @migration_ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
