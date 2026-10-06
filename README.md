# GudangKu

GudangKu is a desktop warehouse management system built with JavaFX and Spring Boot. It helps warehouse teams manage inventory, record stock movements, process deliveries and transfers, and generate operational reports.

## Features

- Manage items, customers, suppliers, and user accounts.
- Record inbound receipts and outbound delivery orders.
- Track inventory by lot and review stock movement history.
- Transfer inventory between warehouse sections.
- Use configurable FIFO, FEFO, or LEFO picking strategies. FEFO is the default.
- Generate inventory and stock movement reports.

## Technology Stack

- Java 17
- JavaFX 21
- Spring Boot 3.2
- Spring Data JPA and Hibernate
- MySQL
- Maven
- JasperReports

## Requirements

- JDK 17 or newer
- MySQL server
- Git, if cloning the repository

## Configuration

The default database connection uses `localhost:3306`, the database name `gudang_akhir`, and the MySQL user `root`. The application can create the database if it does not already exist. Ensure that the MySQL server is running before launching the application.

To use a different database or credentials, configure the Spring datasource properties in `src/main/resources/application.properties` or provide the corresponding Spring Boot environment variables. Do not commit passwords or other credentials to the repository.

The inventory picking strategy can be set with the `PICKING_STRATEGY` environment variable. Supported values are `FIFO`, `FEFO`, and `LEFO`; the default is `FEFO`.

## Run the Application

From the repository root on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```sh
./mvnw spring-boot:run
```

The application opens the GudangKu desktop login screen after startup.

## Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS or Linux:

```sh
./mvnw test
```

## Project Structure

```text
src/
├── main/
│   ├── java/com/mycompany/tugas_akhir/
│   │   ├── model/       Domain models
│   │   ├── repository/  Data access
│   │   └── service/     Inventory and business workflows
│   └── resources/
│       ├── fxml/        JavaFX screens
│       ├── css/         Interface styles
│       ├── reports/     JasperReports templates
│       └── sql/         Database schema and migrations
└── test/                Automated tests
```
