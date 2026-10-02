








USE gudang_akhir;


SET @col1 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA='gudang_akhir' AND TABLE_NAME='users' AND COLUMN_NAME='display_name');
SET @sql1 = IF(@col1=0,
    'ALTER TABLE users ADD COLUMN display_name VARCHAR(100) NULL AFTER full_name',
    'SELECT ''display_name already exists'' AS info');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;


SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA='gudang_akhir' AND TABLE_NAME='users' AND COLUMN_NAME='phone');
SET @sql2 = IF(@col2=0,
    'ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL AFTER email',
    'SELECT ''phone already exists'' AS info');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;


SET @col3 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA='gudang_akhir' AND TABLE_NAME='users' AND COLUMN_NAME='bio');
SET @sql3 = IF(@col3=0,
    'ALTER TABLE users ADD COLUMN bio TEXT NULL AFTER phone',
    'SELECT ''bio already exists'' AS info');
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;


SET @col4 = (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA='gudang_akhir' AND TABLE_NAME='users' AND COLUMN_NAME='profile_picture_path');
SET @sql4 = IF(@col4=0,
    'ALTER TABLE users ADD COLUMN profile_picture_path VARCHAR(500) NULL AFTER bio',
    'SELECT ''profile_picture_path already exists'' AS info');
PREPARE stmt4 FROM @sql4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;


SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='gudang_akhir' AND TABLE_NAME='users'
ORDER BY ORDINAL_POSITION;
