# WMS Proposal — PT Rotaryana Prima

> Reference specification converted from **Proposal Solutions Flow - PT Rotaryana Prima.pdf**.
> Source proposal: **PT. Solusi Kebutuhan Teknologi (PRIEDS)**.
>
> This Markdown is intended as a functional reference for adapting the existing GudangKu/WMS application to the processes and capabilities described in the proposal. Source-derived requirements are preserved; implementation notes are separated where they are inferred from the proposal and the current application structure.

---

## 1. Source Context

- **Solution:** Warehouse Management System (WMS)
- **Client:** PT Rotaryana Prima
- **Provider:** PT. Solusi Kebutuhan Teknologi / PRIEDS
- **Address in proposal:** Indonesia Stock Exchange Tower 1 Level 3, Unit 304, Jl. Jendral Sudirman Kav 52-53, Senayan, Kebayoran Baru, Jakarta Selatan, DKI Jakarta 12190
- **Website:** www.prieds.com

The proposal describes an integrated supply-chain management solution combining **WMS, IoT hardware, and ERP integration** to simplify, improve, and streamline warehouse/business processes and provide insight for business decisions. The proposal also emphasizes an **all-in-one**, **multi-platform**, **adjustable/customizable**, and **end-to-end integrated** system.

Source reference: proposal pages 1–10.

---

## 2. Solution Principles

The application should follow these principles described in the proposal:

### 2.1 Adjustable & Customizable

The system should be adaptable to different business needs and warehouse processes.

### 2.2 Compatibility

The cloud-based system should use a secured backend and be capable of integrating with external systems such as:

- SAP
- Oracle
- Accurate

### 2.3 End-to-End Integration

The WMS should be able to integrate with different systems and hardware through **Internet of Things (IoT)** technology.

### 2.4 Multi-Platform

The system should be accessible from different devices and locations so users can monitor and control warehouse operations without time and space constraints.

Source reference: proposal pages 7 and 10.

---

## 3. Market / Operational Scope Mentioned in the Proposal

The proposal identifies three operational contexts:

### Manufacturing

Use cases:

- Management of finished goods (FG), raw materials (RM), and work in process (WIP)
- Tracking spare parts and consumables
- Integration between production and warehouse systems

Example industries mentioned:

- Mattress
- Automotive
- Electronics

### Distribution

Use cases:

- High-volume outbound and multi-channel distribution
- Fast and accurate fulfillment
- Stock synchronization between branches and marketplaces

Example industries mentioned:

- Distributor
- FMCG
- Consumer Goods

### Logistics & 3PL

Use cases:

- Third-party / multi-principal inventory
- SLA-based delivery and order visibility
- Real-time status tracking
- Custom reports

Example users mentioned:

- Logistics Service Providers
- Cold Storage
- Warehouse Rental

Source reference: proposal page 8.

---

## 4. Core Product Features

The proposal identifies the following WMS product features.

### 4.1 Stock Opname Dynamic Cycle

Support multiple stock-opname cycles:

- **Daily** — stock opname based on daily mutation.
- **Monthly** — stock opname based on stock-opname operating days.
- **Periodic** — stock opname for all SKUs divided among users.

### 4.2 Layout Storage Management 2D

Provide a **2D warehouse/storage layout** for tracking storage locations.

The system should support retrieving specific stock data based on an area in Storage Management.

### 4.3 Putaway Suggestion with CBM

Putaway suggestions should recommend an optimal storage location based on:

- CBM / volume
- Storage type
- Location priority
- Location availability
- Item type

The feature is intended to improve efficient and accurate warehouse operations.

### 4.4 Forecasting

Forecasting should allow users to determine:

- Buffer Stock
- Maximum Inventory Position
- Monthly Average Demand (auto-calculated)

The stated objective is to keep inventory positions available without excessive stock.

### 4.5 Smart Picking: FEFO / LEFO / FIFO

Picking recommendations should use inventory sequencing rules including:

- **FEFO** — First Expired, First Out
- **LEFO** — Last Expired, First Out
- **FIFO** — First In, First Out

The stated objective is to reduce dead stock by recommending appropriate inventory to pick based on expiration date or arrival sequence.

### 4.6 Warehouse Dashboard

The proposal expects a dashboard capable of tracking:

- Order fulfillment rates
- Productivity of each picker
- Supplier SLA in item fulfillment
- Packing efficiency
- Loading efficiency

Source reference: proposal pages 11–12.

---

## 5. General WMS Benefits / Desired Outcomes

The proposal associates the WMS with the following outcomes:

- **Integrated Information** — one system/software to track and trace the supply chain.
- **Improved Productivity** — optimize resources.
- **Saves Money & Time** — optimize inventory, labor, and warehouse space.
- **Data Analysis & Forecasting** — provide real-time insights and support fast data-driven decisions.
- **Increased Efficiency** — automate receiving, putaway, picking, packing, and shipping.
- **Improved Customer Loyalty** — improve order accuracy and reduce shipping errors.

Source reference: proposal page 13.

---

# 6. Functional Workflow Specification

The following sections describe the workflows in the proposal. The implementation should preserve these process stages and their business intent.

---

## 6.1 Master Data Flow

Master data includes:

- Supplier
- Customer
- Item

### Standard import flow

1. Prepare master data in an Excel file using the required format.
2. Log into the system using the user's account.
3. Select the data type to import:
   - Supplier
   - Customer
   - Item
4. Download the corresponding Excel template when needed.
5. Upload the prepared Excel file.
6. System displays a preview of the imported data.
7. User reviews and verifies the preview.
8. User clicks **Import**.
9. System stores the master data and makes it available for warehouse transactions.

### Import requirements

The system should support dedicated import flows for:

- Import Supplier
- Import Customer
- Import Item

The proposal explicitly shows **download template → upload → preview/verification → import → data available in system**.

Source reference: proposal page 15.

---

## 6.2 Master Storage Management

The proposal includes a dedicated **Master Storage Management Setup**.

The storage-management interface is intended to provide visibility into warehouse storage locations and inventory by location. The proposal screenshot shows storage records with fields related to storage/SLOC, stock quantity, reserved quantity, available quantity, and location-oriented information.

The application should therefore maintain storage information as a first-class WMS entity and support querying stock by storage area/location.

Source reference: proposal page 16.

---

# 7. Inbound Processes

## 7.1 Purchasing Flow

The purchasing flow shown in the proposal is:

```text
Material & Purchasing Request
        ↓
Approval Document
        ↓
Purchasing Fulfillment & Tender
        ↓
Inbound Process & Generate QR based on FIFO/FEFO
        ↓
Labeling
        ↓
Putaway Process (via Mobile App)
        ↓
Scan Item Barcode / QR Code
        ↓
Scan Destination Location Barcode
        ↓
Storage Location Update in Inventory WMS
```

### Step 1 — Material & Purchasing Request

The requester creates a purchasing request containing relevant purchase information and remarks.

### Step 2 — Approval Document

Approval is triggered and routed to authorized personnel.

The approval can determine whether the purchasing process may proceed.

### Step 3 — Purchasing Fulfillment & Tender

After approval, the purchasing team can see which purchase requests are eligible to proceed to a **Purchase Order (PO)**.

### Step 4 — Inbound Process & QR Generation

The system generates a QR-code label for each item or pallet.

The label may include essential information such as:

- Item code
- Batch number
- Expiration date
- Other essential traceability information

FIFO/FEFO principles are applied based on the generated QR/barcode process.

### Step 5 — Labeling

Warehouse personnel apply QR-code labels to the received goods.

The labeling process supports traceability and FIFO/FEFO compliance.

### Step 6 — Putaway via Mobile App

Putaway is guided by system suggestions based on:

- Item type
- Volume
- Location availability
- Configured suggestion logic

### Step 7 — Scan Item Barcode / QR Code

The operator scans the item barcode using a handheld scanner or mobile device.

The system verifies:

- Item code
- Batch/lot
- Quantity
- FEFO/FIFO compliance

### Step 8 — Scan Destination Location Barcode

The operator scans the barcode of the destination storage location (SLOC).

### Step 9 — Update Inventory WMS

After a successful putaway:

- Storage location is updated in the WMS.
- Inventory is reflected in real time.

Source reference: proposal pages 18 and supporting purchasing/inbound screenshots on pages 19–22.

---

## 7.2 Inbound Flow

The inbound flow is:

```text
Create PO
  ↓
Inbound Process & Generate QR based on FIFO/FEFO
  ↓
Labeling
  ↓
Putaway Process (via Mobile App)
  ↓
Scan Item Barcode / QR Code
  ↓
Scan Destination Location Barcode
  ↓
Storage Location Update in Inventory WMS
```

### Inbound receiving requirements

1. A Purchase Order exists in the ERP system.
2. Vendor sends goods to the warehouse.
3. Warehouse performs inbound receiving.
4. Receiving team performs initial quantity and condition checks.
5. System generates QR-code labels for each item or pallet.
6. QR label carries item/batch/expiration information.
7. FIFO/FEFO rules are applied to the generated barcode/QR process.
8. Warehouse applies the generated label.
9. Goods are moved to the designated storage location.
10. Putaway suggestions consider item type, volume, and location availability.
11. Operator scans the item barcode.
12. System verifies item code, batch/lot, quantity, and FIFO/FEFO compliance.
13. Operator scans destination SLOC barcode.
14. System validates the destination location.
15. Inventory/storage location is updated in the WMS.
16. Real-time inventory reflects the result.

Source reference: proposal pages 24–28 and the Putaway mobile flow on pages 29–31.

---

## 7.3 Putaway Mobile Flow

The proposal includes a mobile Putaway application.

Expected interaction includes:

- Open Putaway App.
- Select inbound document or task.
- View process/task details.
- Scan barcode.
- Scan SLOC/location barcode.
- Validate the selected item and destination.
- Complete the putaway.
- Display a success state when processing succeeds.

The visual flow in the proposal includes process states, barcode/SLOC scan actions, confirmation, and successful putaway feedback.

Source reference: proposal pages 29–31.

---

# 8. Outbound Processes

## 8.1 Sales Order → Delivery Order Flow

The proposal describes the outbound process as:

```text
Create SO
  ↓
Create Delivery Order
  ↓
Unassign Picklist / Assign Picking Task
  ↓
Picking Process (Picking Task Creation in WMS)
  ↓
Item Picking & Barcode Scanning
  ↓
Checking & Packing
  ↓
Loading Process
  ↓
Dispatch / Goods Issue
```

### Create Sales Order

- Sales Order (SO) is created in the ERP system.
- The Sales team creates the SO based on customer demand or request.

### Create Delivery Order

- System checks item availability.
- A Delivery Order (DO) is generated from the SO.
- The DO identifies items and quantities to be picked and shipped.
- Partial delivery is supported according to the proposal.

### Picklist / Assignment

- WMS generates picking tasks based on delivery instructions.
- System or warehouse supervisor assigns picking tasks to warehouse pickers.
- Assignment may consider:
  - Workload
  - Zone
  - Shift
- Assignment provides accountability and execution tracking.

### Picking Recommendation

- WMS applies FIFO/FEFO logic to determine optimal stock to pick.
- The system displays recommended barcodes and storage locations (SLOC).
- Picker scans the item barcode to confirm the correct item.
- Incorrect item/location scans can trigger an alert or process block.
- Picked items move to a staging area for dispatch preparation.

### Checking & Packing

At the staging area:

- A checking process is performed.
- Operator scans item barcodes again or inputs quantity manually.
- System validates against the DO quantity.
- After successful checking, items are packed into cartons/pallets as needed.
- Shipping labels, delivery documents, and packing lists can be printed.

### Loading

- Operator enters the Loading System module.
- DO barcode is scanned.
- System displays all items scheduled for delivery.
- Operator verifies that all items are physically loaded.

### Dispatch / Goods Issue

- After loading is completed and confirmed, dispatch is submitted.
- The dispatch triggers a Goods Issue (GI) transaction in the system.

Source reference: proposal pages 33–43 and related outbound UI screenshots.

---

## 8.2 Picklist Mobile Flow

The proposal includes a mobile Picklist App for outbound execution.

Expected functions include:

- View picking tasks.
- View pick order details.
- View recommended pick list based on FIFO/FEFO.
- View item and SLOC information.
- Scan item/barcode.
- Validate the scanned item/location.
- Prevent or alert on an invalid scan.
- Mark the picking task as completed.

The mobile workflow is shown visually in the outbound proposal pages 38–41.

---

## 8.3 Consumable Parts / Disposal / Material Usage

The proposal includes a separate outbound flow for material usage/MRO.

```text
Create Material Usage / MRO
  ↓
Approval
  ↓
Unassign Picklist / Assign Picking Task
  ↓
Picking Process (Picking Task Creation in WMS)
  ↓
Item Picking & Barcode Scanning
  ↓
Dispatch MTU
  ↓
Goods Issue / Stock Deduction
```

### Request content

The request includes:

- Item Code
- Required Quantity
- Purpose
- Requester

Examples of internal use mentioned:

- Production
- Maintenance
- Sampling

### Approval

The request can be reviewed by:

- Department head
- Warehouse supervisor
- Authorized personnel

The system may validate against:

- Usage limits
- BOM allocations
- Cost center budgets

Once approved, the request becomes **Ready for Fulfillment**.

### Picking

The picking flow follows WMS task creation and FIFO/FEFO-based stock recommendation.

Incorrect item or location scans may trigger an alert/block.

### Dispatch MTU

After picking:

- Items are staged or delivered to the destination.
- Dispatch MTU is submitted.
- Goods Issue (GI) is triggered for internal usage.
- Stock is deducted and recorded as issued to internal use.

Source reference: proposal pages 44–47 and related screenshots.

---

# 9. Return Processes

## 9.1 Purchase Return

The purchase-return workflow is:

```text
Create DO PO Return
  ↓
Unassign Picklist / Assign Picker
  ↓
Picking Process (Picking Task Creation in WMS)
  ↓
Item Picking & Barcode Scanning
  ↓
Process DO PO Return
  ↓
Goods Issue / Inventory Deduction
  ↓
Status = Dispatched / Returned
```

### DO PO Return

The system creates a Delivery Order for Purchase Order Return.

The document includes:

- Original PO reference
- Vendor name
- Items to be returned
- Quantity to be returned
- Return reason

### Picking

- Warehouse supervisor/system assigns picking tasks.
- WMS generates tasks from the PO Return document.
- FIFO/FEFO logic is applied to determine stock.
- Picker scans item barcodes.
- Wrong scans can trigger an alert or process block.
- Picked items move to the staging area.

### Completing the Return

After picking is completed and verified:

- Operator submits the DO PO Return.
- System triggers a Goods Issue (GI).
- Stock is deducted from warehouse inventory.
- Return document status is updated to **Dispatched/Returned**.

Source reference: proposal pages covering WMS Flow — Return — Purchase Return.

---

## 9.2 Sales Return

The sales-return workflow is:

```text
Create Sales Return
  ↓
Select Delivery Order (DO) to be Returned
  ↓
Submit Sales Return Document
  ↓
Physical Return / Verification / Barcode Handling
  ↓
Putaway Process (via Mobile App)
```

### Create Sales Return

The user (logistics admin or warehouse staff) creates a Sales Return document.

The document records the return of goods from a customer back to the warehouse.

### Select Delivery Order

The user selects the original Delivery Order that shipped the goods.

The system displays the items from that DO.

The user selects:

- Item(s) being returned
- Quantity for each item
- Return reason

Examples of reasons mentioned:

- Damaged
- Incorrect
- Expired

### Submit Sales Return

After details are filled in:

- User submits the Sales Return document.
- System generates a reference receiving document.
- Return status becomes **In Progress**.

### Physical Return / Barcode Handling

When returned goods arrive:

- Warehouse receives goods physically.
- Staff scans the returned item barcode.
- If the original outbound barcode is available, the system recognizes/reuses it.
- If no valid barcode exists or the label is damaged/missing, the system may generate a new barcode.
- Received quantity is checked against the Sales Return document.

### Putaway

- Returned goods are moved from the receiving return area to the designated storage location.
- Putaway is guided by item type, volume, and location availability.

Source reference: proposal pages covering WMS Flow — Return — Sales Return.

---

# 10. Stock Movement Between Warehouses

The proposal describes a stock-transfer process between warehouses.

```text
Create Stock Transfer Plan
  ↓
Approval Documents STO
  ↓
Create Stock Transfer Order
  ↓
Assign / Unassign Picklist
  ↓
Picking Process
  ↓
Item Picking & Barcode Scanning
  ↓
Dispatch STO
  ↓
STO In
  ↓
Receiving Inspection
  ↓
Putaway Process (Mobile App)
  ↓
Destination Inventory Update
```

## 10.1 Stock Transfer Plan

The warehouse/inventory planner creates a transfer plan containing:

- Items
- Quantities
- Source warehouse
- Target warehouse

## 10.2 Approval

The transfer plan is routed for approval to authorized personnel such as:

- Warehouse manager
- Inventory controller

The approver verifies:

- Stock availability
- Transfer justification
- Compliance with inventory policies

Approval triggers Stock Transfer Order creation.

## 10.3 Stock Transfer Order (STO)

The system generates the STO document and captures transfer details.

The user inputs or verifies the quantity to be fulfilled for each SKU.

The STO becomes the official instruction for warehouse picking and preparation.

## 10.4 Picking

- Warehouse supervisor/system assigns pickers.
- WMS creates picking tasks from transfer instructions.
- FIFO/FEFO determines optimal stock.
- Picker scans item barcode.
- Invalid item/location scans may trigger an alert/block.
- Picked items are moved to staging.

## 10.5 Dispatch STO

After final verification in staging:

- Dispatch is submitted.
- Goods Issue (GI) is triggered.

## 10.6 STO In

At the destination warehouse:

- Receiving warehouse physically inspects and verifies incoming goods.
- After receipt submission, destination inventory is updated.

## 10.7 Putaway at Destination

- Goods are moved to designated storage locations.
- Putaway is guided by system suggestions.
- Destination storage location is updated in WMS.
- Inventory reflects the movement in real time.

Source reference: proposal pages covering **WMS Flow — Stock Movement — Stock Transfer Order Between Warehouse**.

---

# 11. Stock Transfer with SLOC (Within the Same Warehouse)

The proposal includes a specific process for moving stock between two storage locations (SLOCs) in the same warehouse.

```text
Initiate Stock Transfer (SLOC → SLOC)
  ↓
Scan Barcode of Item
  ↓
Input Quantity
  ↓
Scan Destination Location Barcode
  ↓
System Update
  ↓
Confirmation
```

## 11.1 Initiate Transfer

Warehouse staff initiates a stock transfer between two SLOCs.

The transfer can be:

- Planned
- Ad-hoc

Operational reasons mentioned include:

- Space optimization
- Zoning
- Other operational needs

## 11.2 Scan Item

Using a handheld scanner/device:

- Operator scans the item barcode.
- System validates the item code.
- System retrieves current quantity and source location.

## 11.3 Input Quantity

Operator specifies the quantity to be moved.

## 11.4 Scan Destination

Operator scans the barcode of the target SLOC.

The system checks whether the destination:

- Is valid
- Is active
- Is suitable for the item based on storage rules

## 11.5 System Update

When the transfer succeeds:

- Stock movement record is updated.
- Quantity is deducted from source SLOC.
- Quantity is added to destination SLOC.
- Movement is timestamped.
- User ID/device is recorded.

## 11.6 Confirmation

The system should show a confirmation similar to:

> “Stock moved successfully from [Source SLOC] to [Destination SLOC].”

The transfer should also be available in audit trail, stock history, or movement reports.

Source reference: proposal section **WMS Flow — Inventory Management — Stock Transfer with SLOC**.

---

# 12. Reporting

The proposal explicitly includes the following reporting areas:

- Inbound Report
- Outbound Report
- Stock Transfer Order Report
- Dashboard Report
- Stock Report

Reports are intended to provide visibility into operational and inventory activities and to support analysis.

## 12.1 Inbound Report

Should support analysis/visibility of inbound activity.

## 12.2 Outbound Report

Should support analysis/visibility of outbound activity.

## 12.3 Stock Transfer Order Report

Should support visibility into inter-warehouse Stock Transfer Order activity.

## 12.4 Dashboard Report

Should summarize operational warehouse performance and key metrics.

The proposal's Warehouse Dashboard feature specifically mentions:

- Order fulfillment rate
- Picker productivity
- Supplier SLA fulfillment
- Packing performance
- Loading performance

## 12.5 Stock Report

Should provide inventory/stock reporting.

Source reference: proposal section titled **Report** and the Warehouse Dashboard feature on page 12.

---

# 13. Customization Requirements

The proposal includes several explicit customizations.

## 13.1 Reserved Stock Quantity

Inventory information should show **reserved stock quantity**.

When the user clicks reserved quantity, the system should display the documents that caused the reservation.

The reference documents mentioned are:

- Sales Order (SO)
- Delivery Order (DO)

The system should also allow the user to view the detailed transaction behind the reservation.

## 13.2 Currency in Purchase Orders

Each Purchase Order should show:

- Currency used
- Currency-related PO detail

## 13.3 Inventory Filtering

Inventory data should be filterable based on:

- Item
- Site
- Currency

## 13.4 Inventory Cost

Inventory may display the **total cost and currency for each item**.

## 13.5 Average Activity View

Inventory should provide a view showing the **average of inbound and outbound activities**, including the ability to view the information by site.

## 13.6 Stock Suggestions per Item

Stock suggestions should be configurable per item.

The calculation/formula can be defined according to PT Rotaryana Prima's requirements.

The proposal explicitly describes this as a customizable rule/formulation.

Source reference: proposal section **Customization Flow and Report**.

---

# 14. Standard Features

The proposal lists the following standard features.

### Master Data / Core

- Product Management
- Customer Management
- Supplier Management
- Storage Management
- Inventory

### Inbound / Putaway

- Inbound
- QC Inbound
- Putaway App (Mobile)
- Put Away Report

### Outbound

- Split Stock
- Create Delivery Order
- Delivery Order Report
- Unassigned Picklist
- Picklist App (Mobile)
- Packing System
- Purchase Return
- Sales Return

### Transfer / Inventory Operations

- Approval Document
- Stock Transfer Request (Central Vault & Remote Vault)
- STO Report
- Dispatch STO
- STO In
- Stock Transfer with SLOC
- Stock Opname
- Process Opname (Mobile)
- Stock Transfer Between Barcode
- Material Usage with Picklist

### Warehouse Visualization / Reports

- Point of Sales
- 2D Layout Warehouse
- Stock Report
- Warehouse Dashboard

Source reference: proposal **Standard Features** section.

---

# 15. Advanced Features

The proposal lists these advanced/configurable features:

- Assembly / Disassembly
- Manufacturing Accurate Online
- FEFO Mode (Edit Inventory Batch & ED and QC FEFO)
- Stock Suggestion to Create PR
- Configuration Available Qty
  - Exclude Zone for selected transaction types, including:
    - Stock transfer with SLOC
    - Stock transfer between barcode
    - Delivery Order (DO)
  - Picklist Zone for:
    - Stock transfer
    - Purchase Return
- Goods Loan with Picklist
- Loading System
- Sync Sales Invoice Accurate (configuration)
- DO printout with company logo (configuration)
- Custom barcode code
- Barcode Generator Inbound + Scan Barcode Inbound (configuration)
- Picklist App Split Stock (configuration)
- 2-step verification login (configuration)
- Outbound Process DO Receive (configuration)
- QC Inbound FIFO (setting/configuration)
- Stock Transfer Plan (setting/configuration)
- Material Usage Project Sync from Accurate Online (setting/configuration)
- Approval Management by User Department (manual setup required)
- Putaway Suggestion

Source reference: proposal **Advanced Features** section.

---

# 16. Data / Inventory Behavior

Based on the proposal workflows, inventory data should support these operational characteristics:

- Real-time quantity updates after successful inbound/putaway.
- Real-time stock deduction after outbound Goods Issue.
- Source and destination SLOC tracking.
- Barcode-based item identification.
- Batch/lot tracking where applicable.
- Expiration-date tracking where applicable.
- FIFO/FEFO-based stock selection.
- Reserved quantity tracking.
- Stock movement history.
- User/device traceability for stock movements.
- Inventory by site/storage location.
- Ability to distinguish available stock, reserved stock, and operationally unavailable stock according to configured rules.

Where a rule is explicitly configurable in the proposal, implementation should expose it as configuration rather than hardcoding a single business behavior.

---

# 17. Barcode / QR Requirements

Barcode/QR is a central operational mechanism in the proposal.

The system should support:

- Barcode/QR generation during inbound.
- Item barcode/QR scanning.
- Destination SLOC barcode scanning.
- Validation of item and location against the active task.
- Reuse of the original barcode on eligible sales returns.
- Generation of a new barcode when the original return barcode is invalid/missing.
- Custom barcode code configuration.
- Barcode-based movement tracking.

Incorrect barcode/location scans may result in:

- Alert
- Process block

according to the workflow.

---

# 18. Mobile Application Requirements

The proposal uses mobile apps for warehouse execution, especially:

- Putaway App
- Picklist App
- Process Opname (Mobile)

Mobile workflows should prioritize:

- Barcode scanning
- SLOC scanning
- Task/document selection
- Clear process state
- Validation before completion
- Success/error feedback
- Minimal input during physical warehouse operations

The screenshots in the proposal show mobile interfaces designed for step-by-step warehouse execution.

Source reference: proposal pages 29–31 and 38–41, plus the Standard Features section.

---

# 19. Approval Management

Approval is used in multiple business flows, including:

- Purchasing request
- Stock Transfer Plan / STO
- Material Usage / MRO

The proposal states that approvals can be configured by **user department**, with manual setup required for Approval Management.

The implementation should therefore support:

- Approval state
- Approver role/user
- Department-based approval configuration
- Approval history
- Blocking the next process stage until required approval is completed

Only configurations explicitly supported by the proposal should be treated as mandatory.

---

# 20. ERP / External-System Integration

The proposal identifies integration with enterprise systems, including:

- SAP
- Oracle
- Accurate

Specific integration/customization items listed are:

- Manufacturing Accurate Online
- Sync Sales Invoice Accurate
- Material Usage Project Sync from Accurate Online

The WMS should be structured so that ERP-originated documents such as SO/PO can feed downstream warehouse processes while warehouse execution can return operational/inventory status to the integrated system where configured.

---

# 21. Warehouse Dashboard Requirements

The proposal explicitly defines a Warehouse Dashboard as a product feature.

The dashboard should provide operational visibility into:

- Order fulfillment rate
- Picker productivity
- Supplier SLA in item fulfillment
- Packing efficiency
- Loading efficiency

The dashboard is a monitoring/analysis layer, not a replacement for the transaction workflows.

### Recommended logical dashboard areas based on the proposal

> The following grouping is an implementation-oriented interpretation of the proposal's dashboard/reporting content, not a verbatim layout requirement from the PDF.

1. **Overview**
   - Fulfillment summary
   - Inventory/stock status
   - Inbound/outbound summary
   - Key operational indicators

2. **Picking & Productivity**
   - Picker productivity
   - Picking status
   - Completed vs pending tasks

3. **Supplier / Fulfillment / Packing / Loading**
   - Supplier SLA
   - Packing progress
   - Loading progress
   - Order fulfillment status

The exact visual arrangement may be configurable because the proposal emphasizes adjustable/customizable behavior.

---

# 22. Mapping to the Existing GudangKu Application

> This section is an implementation adaptation based on the current GudangKu UI discussed alongside this document. It is not a direct quote from the proposal.

## 22.1 Existing top-level modules

The current application already exposes:

- Dashboard
- Storage
- Movements
- Customer
- Report
- Setting

These modules can be evolved toward the proposal's WMS scope.

## 22.2 Storage

The existing Storage module should evolve toward **Master Storage Management + Inventory Visibility**.

Relevant proposal capabilities to surface:

- Storage/location management
- Stock by storage location
- Reserved quantity visibility
- Current/available stock
- Stock status
- 2D warehouse layout
- Putaway suggestions
- Batch/expiration-aware stock selection

## 22.3 Movements

The existing Movements module should evolve toward a broader inventory movement center.

Possible transaction families from the proposal:

- Inbound movement
- Outbound movement
- Material usage
- Purchase return
- Sales return
- Stock transfer between warehouses
- Stock transfer between SLOCs
- Barcode-based stock transfer

The movement history should preserve:

- Transaction code
- Type
- Item
- Quantity
- Date/time
- Source location where applicable
- Destination location where applicable
- User/device/audit information where applicable

## 22.4 Customer

Customer Management is explicitly listed as a standard feature.

The existing Customer module can remain the primary master-data interface for customer entities and Sales Return references.

## 22.5 Report

The current Report module should align with the proposal's report families:

- Inbound Report
- Outbound Report
- Stock Transfer Order Report
- Dashboard Report
- Stock Report

The report layer should allow filtering and should expose the relevant operational context for each report.

## 22.6 Dashboard

The existing dashboard should align with the proposal's Warehouse Dashboard concept.

At minimum, the implementation should support visibility into:

- Order fulfillment
- Picker productivity
- Supplier SLA
- Packing
- Loading
- Inventory/stock status
- Inbound/outbound activity

---

# 23. UI / Table Design Alignment for the Existing Application

> This is an implementation note based on the current GudangKu interface, not a direct PDF requirement.

For Storage, Movements, Customer, and Report tables:

- Use the full available table/container width.
- Avoid large unused horizontal space.
- Keep only relevant fields in the primary table view.
- Make flexible columns consume remaining horizontal space.
- Use ellipsis/wrapping/tooltip for long content.
- Keep action columns compact.
- Keep operational identifiers and quantities easy to scan.
- Use responsive layouts across desktop and smaller viewports.

This supports the proposal's focus on efficient warehouse monitoring and reporting without changing the proposal's business processes.

---

# 24. Functional State Rules

The implementation should preserve clear state transitions.

Examples:

### Purchasing

```text
Draft Request
→ Submitted
→ Approved
→ Eligible for PO
→ PO Created
→ Inbound
→ Putaway
→ Inventory Updated
```

### Outbound

```text
Sales Order
→ Delivery Order
→ Picking Task
→ Picking
→ Checked
→ Packed
→ Loaded
→ Dispatch / Goods Issue
```

### Purchase Return

```text
PO Return
→ Picking Task
→ Picking
→ Verified
→ Dispatch / Goods Issue
→ Returned
```

### Sales Return

```text
Sales Return
→ DO Selected
→ Submitted
→ In Progress
→ Physical Receipt
→ Verification
→ Putaway
→ Inventory Updated
```

### Inter-Warehouse Stock Transfer

```text
Stock Transfer Plan
→ Approval
→ STO
→ Picking
→ Dispatch STO
→ STO In
→ Receiving Verification
→ Putaway
→ Destination Inventory Updated
```

### SLOC-to-SLOC Transfer

```text
Initiate Transfer
→ Scan Item
→ Input Qty
→ Scan Destination SLOC
→ System Update
→ Confirmation
```

These state sequences are derived from the workflow descriptions and should not be bypassed when the related process is enabled.

---

# 25. Validation Rules

Based on the proposal workflows, validation is required at operational checkpoints.

## Item validation

Validate:

- Item code
- Barcode/QR
- Batch/lot where applicable
- Quantity
- FIFO/FEFO compliance where applicable

## Location validation

Validate:

- Destination location exists
- Destination location is active
- Item is allowed in the destination according to configured storage rules
- Scanned location matches the intended task where applicable

## Quantity validation

Compare operational quantities against the source document where required, including:

- Delivery Order quantity during checking
- Sales Return quantity during return receiving
- Stock Transfer quantity during transfer processing
- Inbound quantity during receiving

## Approval validation

Do not allow dependent processing to proceed until required approval is completed.

## Invalid scan behavior

Where specified in the proposal, an invalid item/location scan should:

- Alert the operator, and/or
- Block completion of the operation

---

# 26. Auditability / Traceability

The proposal emphasizes traceability throughout the warehouse process.

The system should retain operational records for:

- Transaction/document reference
- User
- Device where applicable
- Timestamp
- Source SLOC
- Destination SLOC
- Item
- Quantity
- Status transitions
- Barcode/QR events
- Approval activity

This is especially important for stock movement, returns, picking, putaway, and inter-warehouse transfers.

---

# 27. Configuration Areas

The proposal repeatedly identifies business rules as configurable. Configuration should therefore be organized into explicit settings rather than hidden constants.

Potential configuration groups derived from the proposal:

- FEFO/FIFO behavior
- QC Inbound FIFO
- Putaway suggestion logic
- Stock suggestion formula per item
- Stock suggestion to create PR
- Available-quantity calculation
- Excluded zones
- Picklist zones
- Approval workflow by department
- 2-step login verification
- Barcode generation/code format
- Loading process
- Outbound DO receive process
- Currency display
- Accurate Online synchronization
- Company logo in DO printout
- Split-stock behavior

---

# 28. Non-Negotiable Business Concepts From the Proposal

When adapting the application, preserve these business concepts unless the implementation explicitly marks them as disabled/configurable:

1. Warehouse location/SLOC is a core inventory dimension.
2. Barcode/QR scanning is part of physical execution.
3. FIFO/FEFO is used for relevant inventory selection.
4. Putaway is location-aware and can be suggestion-driven.
5. Picking is task-based.
6. Receiving and outbound checks validate against documents.
7. Inventory updates occur after successful process completion.
8. Stock transfer can occur both between warehouses and between SLOCs.
9. Returns are separate inbound/outbound business flows.
10. Approval can be required before downstream execution.
11. Inventory can contain reserved quantities linked to SO/DO references.
12. Reporting includes inbound, outbound, stock transfer, dashboard, and stock reports.
13. The system is designed for ERP integration and configurable business rules.

---

# 29. Suggested Implementation Priority

> This ordering is an implementation-oriented recommendation based on dependencies in the proposal, not a priority ranking stated by the source document.

### Foundation

1. Authentication / user roles
2. Master Data: Item, Customer, Supplier
3. Storage / SLOC master
4. Inventory ledger
5. Barcode/QR identity

### Inbound

6. PO/inbound references
7. Receiving and QC
8. QR generation/labeling
9. Putaway
10. Mobile putaway workflow

### Outbound

11. Delivery Order
12. Picklist / picking task
13. FIFO/FEFO picking recommendation
14. Barcode picking
15. Checking
16. Packing
17. Loading
18. Goods Issue / dispatch

### Transfers / Returns

19. Stock transfer plan
20. STO
21. STO In
22. SLOC-to-SLOC transfer
23. Purchase return
24. Sales return
25. Material usage / MRO

### Reporting / Dashboard

26. Stock Report
27. Inbound Report
28. Outbound Report
29. STO Report
30. Warehouse Dashboard
31. Inventory reservation details
32. Custom analytics

### Advanced / Integration

33. ERP integration
34. Accurate Online integration
35. Advanced configuration
36. Stock suggestion formulas
37. Advanced verification/security features

---

# 30. Reference Page Guide

For implementation, the following pages are especially useful as references:

| Proposal page | Reference |
|---|---|
| 7 | Digitize Your Business with PRIEDS; adjustable/customizable, compatibility, end-to-end integration, multi-platform |
| 8 | Market focus: Manufacturing, Distribution, Logistics & 3PL |
| 11 | Stock Opname Dynamic Cycle, 2D Storage Layout, Putaway Suggestion with CBM |
| 12 | Forecasting, Smart Picking FEFO/LEFO/FIFO, Warehouse Dashboard |
| 13 | WMS benefits/outcomes |
| 15 | Master Data import flow |
| 16 | Master Storage Management setup |
| 18 | Purchasing flow |
| 24 | Inbound flow |
| 29–31 | Mobile Putaway flow |
| 33 | Outbound SO → DO flow |
| 38–41 | Mobile Picking / outbound execution examples |
| 44 | Material Usage / MRO outbound flow |
| Return section | Purchase Return and Sales Return workflows |
| Stock Movement section | Stock Transfer Order between warehouses |
| Inventory Management section | Stock Transfer with SLOC |
| Report section | Inbound, Outbound, STO, Dashboard, and Stock Reports |
| Customization section | Reserved stock, currency, cost, activity average, stock suggestions |
| Features section | Standard and Advanced Features |

---

# 31. Acceptance Checklist

Use this checklist when validating an implementation against the proposal.

## Master Data

- [ ] Supplier import from Excel
- [ ] Customer import from Excel
- [ ] Item import from Excel
- [ ] Template download
- [ ] Preview/verification before import

## Storage / Inventory

- [ ] Storage/SLOC management
- [ ] Inventory by storage location
- [ ] Reserved quantity
- [ ] Reservation document drill-down to SO/DO
- [ ] 2D layout reference
- [ ] Real-time inventory update

## Inbound

- [ ] PO reference
- [ ] Receiving quantity/condition check
- [ ] QR generation
- [ ] Item/batch/expiration information
- [ ] FIFO/FEFO handling
- [ ] Putaway suggestion
- [ ] Mobile putaway
- [ ] Item barcode scan
- [ ] Destination SLOC scan

## Outbound

- [ ] SO → DO process
- [ ] Picking task creation
- [ ] Picker assignment
- [ ] FIFO/FEFO picking recommendation
- [ ] Barcode scan validation
- [ ] Checking against DO
- [ ] Packing
- [ ] Loading
- [ ] Dispatch / Goods Issue

## Material Usage

- [ ] Material Usage/MRO request
- [ ] Approval
- [ ] Picklist/picking
- [ ] Barcode scanning
- [ ] Dispatch MTU
- [ ] Goods Issue / stock deduction

## Returns

- [ ] Purchase Return
- [ ] Sales Return
- [ ] Original PO/DO references
- [ ] Return reason
- [ ] Barcode reuse/new barcode handling
- [ ] Return receiving verification
- [ ] Return putaway

## Stock Transfer

- [ ] Stock Transfer Plan
- [ ] Approval
- [ ] STO creation
- [ ] Picker assignment
- [ ] Picking
- [ ] Dispatch STO
- [ ] STO In
- [ ] Destination receiving verification
- [ ] Destination putaway
- [ ] SLOC-to-SLOC transfer
- [ ] Barcode-based SLOC transfer
- [ ] Movement audit trail

## Reports / Dashboard

- [ ] Inbound Report
- [ ] Outbound Report
- [ ] STO Report
- [ ] Stock Report
- [ ] Warehouse Dashboard
- [ ] Fulfillment metrics
- [ ] Picker productivity
- [ ] Supplier SLA
- [ ] Packing/load visibility

## Configuration / Integration

- [ ] ERP integration architecture
- [ ] Accurate integration where enabled
- [ ] Configurable stock suggestion
- [ ] Configurable putaway suggestion
- [ ] Configurable zones
- [ ] Configurable approval management
- [ ] Configurable FEFO/FIFO/QC behavior
- [ ] Barcode configuration

---

# 32. Source Integrity Notes

- This document is derived from the uploaded proposal and keeps the proposal's terminology such as **WMS, SLOC, STO, DO, SO, PO, GI, FIFO, FEFO, LEFO, Putaway, Picklist, Material Usage/MRO, QC Inbound, Warehouse Dashboard, and Stock Opname**.
- Some proposal pages are primarily screenshots/mockups. Visual examples are treated as UI/process references rather than as additional business rules unless the accompanying text explicitly describes the behavior.
- The **"Mapping to the Existing GudangKu Application"**, **UI/table alignment**, and **Suggested Implementation Priority** sections are implementation adaptations added to make the source easier to apply to the existing app. They are not presented as verbatim requirements from the proposal.
- Where the proposal says a feature is configurable, the implementation should preserve configurability instead of hardcoding a single behavior.

---

## End of Specification

**Primary source:** `Proposal Solutions Flow - PT Rotaryana Prima.pdf`
