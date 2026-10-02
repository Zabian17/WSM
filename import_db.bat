@echo off
setlocal enabledelayedexpansion

set MYSQL_PATH=C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysql.exe
set HOST=localhost
set USER=root
set PASSWORD=12345
set DATABASE=gudang_akhir
set SQL_FILE=c:\laragon\www\tugas_akhir.worktrees\agents-file-check-and-validation\gudang_akhir_updated.sql

echo.
echo ======================================
echo Creating new database...
echo ======================================
echo DROP DATABASE IF EXISTS %DATABASE%; CREATE DATABASE %DATABASE%; | "%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD%

if errorlevel 1 (
    echo ❌ Failed to create database
    exit /b 1
)

echo ✅ Database created successfully
echo.

echo ======================================
echo Importing SQL file...
echo ======================================
"%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD% %DATABASE% < "%SQL_FILE%"

if errorlevel 1 (
    echo ❌ Import failed
    exit /b 1
)

echo ✅ SQL file imported successfully
echo.

echo ======================================
echo Removing duplicate data...
echo ======================================

REM Remove duplicate suppliers
echo Cleaning duplicate suppliers...
(
echo USE %DATABASE%;
echo DELETE FROM supplier WHERE id NOT IN (SELECT MIN(id) FROM (SELECT MIN(id) as id FROM supplier GROUP BY LOWER(REPLACE(nama_supplier, ' ', ''^')) AS min_ids);
) | "%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD%

REM Remove duplicate rak
echo Cleaning duplicate rak...
(
echo USE %DATABASE%;
echo DELETE FROM rak WHERE id NOT IN (SELECT MIN(id) FROM (SELECT MIN(id) as id FROM rak GROUP BY LOWER(kode_rak)) AS min_ids);
) | "%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD%

REM Remove duplicate barang
echo Cleaning duplicate barang...
(
echo USE %DATABASE%;
echo DELETE FROM barang WHERE id NOT IN (SELECT MIN(id) FROM (SELECT MIN(id) as id FROM barang GROUP BY LOWER(nama_barang)) AS min_ids);
) | "%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD%

echo.
echo ✅ Database import and cleanup completed!
echo.
echo Summary:
(
echo USE %DATABASE%;
echo SELECT 'Supplier records:' as info, COUNT(*) as count FROM supplier UNION ALL SELECT 'Rak records:', COUNT(*) FROM rak UNION ALL SELECT 'Barang records:', COUNT(*) FROM barang UNION ALL SELECT 'User records:', COUNT(*) FROM user^;
) | "%MYSQL_PATH%" -h %HOST% -u %USER% -p%PASSWORD%

echo.
echo Done! You can now export from phpMyAdmin.
pause
