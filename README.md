# GudangKu

GudangKu is a Java desktop warehouse management system (WMS) for managing
inventory, inbound and outbound operations, warehouse transfers, and day-to-day
stock control. The desktop interface is built with JavaFX; Spring Boot and
Spring Data JPA provide the application and persistence layers, with MySQL as
the database.

The application currently targets desktop workflows. It does not include a
mobile application, REST/HTTP API, or live ERP integration.

## Capabilities

### Foundation and inventory

- User registration and login with BCrypt password hashing, startup migration
  of legacy plaintext passwords, and login-time migration of salted SHA-256 hashes.
- Admins can reset another user's password from Settings; new and reset passwords
  require at least 12 characters and no more than 72 UTF-8 bytes.
- Item, customer, supplier, warehouse-section, and rack management.
- Inventory balances by storage location (SLOC), batch, and expiry.
- Stock-in and stock-out movements recorded in the inventory ledger.
- Stock transfers between SLOCs with lot allocation history and optional
  keyboard-wedge verification of item QR plus source/destination SLOC barcodes.
- FIFO, FEFO, or LEFO lot-allocation configuration shared by stock issues,
  SO/DO reservations, MRO Goods Issue, and STO dispatch. FEFO is the default.
- Interactive schematic warehouse map, inventory filters, and stock alerts.
- Desktop stock count by rack, including book-stock snapshot, physical count,
  variance review, guarded posting, and on-demand count-priority
  recommendations.
- Excel `.xlsx` import and template support for item, customer, and supplier
  master data. Invalid rows block the complete import; imported item stock
  starts at zero.

### Inbound and purchasing

- Purchase orders with maker-checker approval and receipt linkage.
- Inbound receipt, quality-control review, and putaway workflow.
- Putaway recommendations based on target section category and rack capacity.
- Desktop keyboard-wedge scan validation for item QR and destination SLOC
  barcode. Item scans validate SKU, batch, and expiry.
- QR label generation for receipt items and warehouse locations.

### Outbound

- Sales orders converted to delivery orders (SO to DO), preserving source
  document references and remaining quantities.
- DO release, strategy-based lot reservation, pick task creation, selectable
  picker assignment, and section assignment.
- Desktop picking scan validation for item QR, SLOC, batch, expiry, assigned
  picker, and section.
- Packing and loading quantity checks with operator/time audit.
- Dispatch and Goods Issue through the inventory ledger, including partial
  delivery support.

### Material usage, returns, and inter-warehouse transfers

- Material Usage/MRO requests with maker-checker review and configured lot
  allocation during Goods Issue. Multi-item requests can issue progressively
  using item QR, source SLOC, quantity verification, and persisted scan history.
- Purchase returns linked to received PO lines and sales returns linked to
  dispatched DO lines.
- Return review, reason tracking, source-quantity checks, and ledger posting.
- Optional return scans validate purchase-return item/lot/source SLOC or
  sales-return item/destination SLOC before posting.
- Posted single-item sales returns can reuse an existing item QR payload or
  generate/reprint a matching replacement label.
- Warehouse master and section-to-warehouse mapping.
- Stock Transfer Plan / STO with approval, picker assignment, keyboard-wedge
  item/SLOC verification, partial dispatch and receipt, scan history, and
  batch/expiry-preserving ledger posting.

### Reporting

- Transaction reports and recent operational views for inbound/QC,
  outbound fulfillment, STO, pick scans, picker activity, supplier QC
  turnaround, PO-to-receipt delivery lead time, and packing/loading audit.
- Operational report filters by inclusive date range and category. Results
  remain bounded to the most recent repository windows documented below.
- CSV export for transaction and operational reports; PDF export for filtered
  transaction reports.

Operational reports support user-selected inclusive date ranges, but searches
remain bounded to recent repository windows.

## Technology

| Component | Technology |
| --- | --- |
| Language and runtime | Java 25 |
| Desktop UI | JavaFX 25, FXML, AtlantaFX Primer Light |
| Application framework | Spring Boot 3.5.16 |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8-compatible server and MySQL Connector/J |
| Build | Maven Wrapper |
| Password hashing | Spring Security Crypto (BCrypt) |
| QR/barcodes | ZXing 3.5.3 |
| Excel import | Apache POI 5.4.1 |
| Reporting library | JasperReports 6.21.3 |
| Automated tests | JUnit 5, Mockito, Spring Boot Test |

## Requirements

- JDK 25 or newer. The project compiles with Java 25 language/API settings.
- MySQL server accessible from the development machine.
- Network access on the first build so Maven can download dependencies.
- Git, if cloning the repository.

## Database configuration

The default development connection is:

- Host: `localhost`
- Port: `3306`
- Database: `gudang_akhir`
- Username: `root`

The default JDBC URL requests creation of the database if it does not already
exist. The application uses Hibernate schema update for local development.
For a new database, the canonical schema is also available in
[`src/main/resources/sql/schema.sql`](./src/main/resources/sql/schema.sql).

Local runs use the `dev` profile by default. It enables Hibernate schema update
and defaults to Laragon's local `root` account without a password. For a
production-like run, activate a profile other than `dev`; the base configuration
requires `DB_USER` and `DB_PASSWORD`, requires encrypted database connections,
and validates rather than modifies the schema.

Connection values can be overridden with environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `gudang_akhir` | Database name |
| `DB_USER` | `root` in `dev`; required otherwise | Database username |
| `DB_PASSWORD` | Empty in `dev`; required otherwise | Database password |
| `DB_SSL_MODE` | `DISABLED` in `dev`; `REQUIRED` otherwise | MySQL TLS mode (`REQUIRED`, `VERIFY_CA`, `VERIFY_IDENTITY`, or `DISABLED` for local development only) |

For example, in PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "gudang_akhir"
$env:DB_USER = "root"
$env:DB_PASSWORD = "your-local-password"
$env:DB_SSL_MODE = "VERIFY_IDENTITY"
$env:SPRING_PROFILES_ACTIVE = "prod"
```

Use `VERIFY_IDENTITY` for remote databases with a trusted server certificate.
Use `REQUIRED` when encryption is needed but certificate verification is not
configured. Set `DISABLED` only for a local development server without TLS.

Do not commit credentials or production data. The bundled
[`database/gudang_akhir.sql`](./database/gudang_akhir.sql) is a local
development export, not a production backup.

### SQL migrations

Flyway manages schema changes from version 15 onward. On startup it adopts an
existing non-empty database at baseline 15, without replaying the historical
manual scripts. A new empty database receives the v15 table schema without
development seed records. Add new
changes as `V16__description.sql` and later versions in
`src/main/resources/db/migration`. If an existing database is incomplete,
Hibernate validation will fail after adoption; repair it from a verified backup
before raising its baseline.

The scripts in `src/main/resources/sql/` document historical manual changes
through `migration_v15.sql`; Flyway does not replay them. Back up databases
before schema updates.

The helper
[`scripts/windows/import_db.bat`](./scripts/windows/import_db.bat) resets and
imports the local development database. It is destructive to the selected
database; do not use it with valuable or production data.

## Run the desktop application

Start MySQL first, then open PowerShell in the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The JavaFX login window should open after Spring Boot has initialized. Maven
downloads required dependencies on the first run.

The mobile API listens on `127.0.0.1:8765` by default. Its OpenAPI contract is
[`docs/mobile-api-v1.yaml`](./docs/mobile-api-v1.yaml). To expose it on a
warehouse network, set `MOBILE_API_BIND_ADDRESS` to the server's LAN address,
enable `MOBILE_API_TLS_ENABLED=true`, and configure the standard Spring Boot
`SERVER_SSL_KEY_STORE`, `SERVER_SSL_KEY_STORE_PASSWORD`, `SERVER_SSL_KEY_STORE_TYPE`,
and `SERVER_SSL_KEY_ALIAS` environment variables. Startup rejects non-loopback
binding without TLS.

An optional Windows launcher is available:

```powershell
.\scripts\windows\run.ps1
```

In NetBeans, the project action in `nbactions.xml` is configured to launch the
JavaFX application.

To change the stock-picking strategy, set `PICKING_STRATEGY` to `FEFO`, `FIFO`,
or `LEFO` before startup. The shared strategy applies to unscanned stock issues,
DO lot reservations, MRO Goods Issue, and STO dispatch. Invalid values stop
application startup; if unset, the application uses `FEFO`.
Inbound QC is required by default. Set `QC_REQUIRED=false` only when quality
checks are performed and documented outside GudangKu; receipts then proceed
directly to putaway and are still not posted to stock until putaway completes.

## Run tests

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS or Linux:

```sh
./mvnw test
```

The test suite covers inventory posting, inbound receipt, delivery orders,
purchase orders, sales orders, cycle count, imports, Material Usage, returns,
STO, and operational reports. The tests use mocks for persistence and do not
replace integration testing against MySQL.

## Project layout

```text
database/                         Local development database export
scripts/
  release/                        Optional Git release helpers
  windows/                        Windows run, build, and DB helpers
src/
  main/
    java/com/mycompany/tugas_akhir/
      config/                     Spring/JavaFX integration
      model/                      JPA domain entities
      repository/                 Spring Data repositories
      service/                    Business workflows and stock ledger
      *Controller.java            JavaFX screen controllers
    resources/
      css/                        JavaFX styles
      fxml/                       JavaFX screens
      sql/                        Canonical schema and SQL migrations
  test/java/                      Unit and service tests
```

## Current limitations

- Desktop-only: no mobile application, camera-based scanning, or REST/HTTP API.
- QR/barcode entry is designed for keyboard-wedge scanners; physical scanner
  hardware has not been integration-tested.
- PO, MRO, returns, and STO approvals now use an admin-managed department
  routing matrix in Settings. Admins assign each user's department and route a
  request department to a distinct approver department; missing routes block
  approval. Existing users need department assignments and routes configured
  after applying `migration_v14.sql`.
- MRO requests support multiple items; Goods Issue scanning progresses each
  line against verified lots and a dedicated picker-assigned pick task. Stock
  is not reserved while the task is open; availability is revalidated at issue.
- STO plans currently support one item per document and do not support
  multi-picker/shift scheduling.
- Return barcode verification and sales-return QR reprint are desktop features;
  camera/mobile scanning and multi-item label batches are not implemented.
- STO scan history supports partial dispatch and receipt, but does not create
  a separate pick-task lifecycle or support multi-picker/shift scheduling.
- Operational reports are limited to recent records (up to 200 receipts,
  100 delivery orders, 200 STOs, and 500 scans/audit rows); date filters apply
  within those limits. Supplier lead-time metrics include only receipts linked
  to a purchase order and do not account for business calendars or target SLAs.
- Advanced ERP integration, including SAP, Oracle, and Accurate, is not
  implemented.
- Database migrations and full workflows have not been validated against a
  live MySQL environment.
