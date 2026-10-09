@echo off
setlocal

set "PROJECT_ROOT=%~dp0..\.."
set "DATABASE=gudang_akhir"
set "SQL_FILE=%PROJECT_ROOT%\database\gudang_akhir.sql"
set "MYSQL_BIN=%MYSQL_BIN%"
set "DB_HOST=%DB_HOST%"
set "DB_PORT=%DB_PORT%"
set "DB_USER=%DB_USER%"

if not defined MYSQL_BIN set "MYSQL_BIN=mysql"
if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_USER set "DB_USER=root"

if not exist "%SQL_FILE%" (
    echo ERROR: Database dump not found: "%SQL_FILE%"
    exit /b 1
)

echo WARNING: This permanently drops and recreates the "%DATABASE%" database.
echo All existing data in that database will be lost.
set /p "CONFIRM=Type %DATABASE% to continue: "
if /I not "%CONFIRM%"=="%DATABASE%" (
    echo Import cancelled.
    exit /b 1
)

echo Resetting database "%DATABASE%"...
"%MYSQL_BIN%" --host="%DB_HOST%" --port="%DB_PORT%" --user="%DB_USER%" -p -e "DROP DATABASE IF EXISTS %DATABASE%; CREATE DATABASE %DATABASE%;"
if errorlevel 1 (
    echo ERROR: Database reset failed.
    exit /b 1
)

echo Importing "%SQL_FILE%"...
"%MYSQL_BIN%" --host="%DB_HOST%" --port="%DB_PORT%" --user="%DB_USER%" -p "%DATABASE%" < "%SQL_FILE%"
if errorlevel 1 (
    echo ERROR: SQL import failed.
    exit /b 1
)

echo Database import completed.
