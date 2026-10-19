# WMS Proposal Tasks

## Phase 1: Complete stock cycle counting

- [x] Include today's posted stock-in/out transactions and SLOC transfer source/destination events in rack priority recommendations.
- [x] Include posted stock-in/out transactions, SLOC transfer source/destination events, and STO activity (through its posted stock transactions) in the daily count signal.
- [x] Add monthly count schedules using Monday–Friday operating days and start-time snapshots.
- [x] Allocate periodic SKU count work across users and record each operator.
- [x] Record each posted variance in the stock ledger with its lot allocation and posting user.
- [x] Checkpoint: cycle-count modes, assignments, variance posting, and audit work together.

## Phase 2: Complete inbound and purchasing

- [x] Add purchase-order currency, line pricing, tender reference, payment terms, and expected delivery date.
- [x] Add configurable oldest-first FIFO ordering for pending inbound QC receipts.
- [x] Complete configurable inbound QC FIFO; existing lot selection supports configured FIFO/FEFO/LEFO for outbound allocation.
- [x] Add item/rack CBM data and capacity-aware putaway ranking.
- [x] Configure section zones, storage types, category rules, putaway priority, and item storage requirements.

## Phase 3: Complete outbound execution

- [x] Add multi-picker, per-section tasks, and shift-aware assignment.
- [x] Add task reservations and item-level checking against delivery orders.
- [x] Export a printable DO packing list with DO Code128 and packed-lot QR labels.
- [x] Add barcode-based packing, loading, and dispatch confirmation to mobile DO fulfillment.
- [x] Complete MRO stock reservation and picker task lifecycle.

## Phase 4: Forecasting and stock suggestions

- [x] Calculate monthly average demand from outbound history across configurable completed calendar months.
- [x] Add per-item buffer/max inventory inputs and suggestions that account for reserved stock.

## Phase 5: Dashboard and reporting

- [x] Add fulfillment, picker productivity, supplier turnaround, packing, and loading metrics to the dashboard.
- [x] Add shared configurable fulfillment, packing, loading, supplier-time, and picker-output targets.
- [x] Add selectable dashboard periods (7/30/90 days, current month, and custom dates).

## Phase 6: Mobile execution

- [x] Select a responsive web mobile client and build its authenticated, paginated API boundary.
- [x] Implement mobile putaway, DO picking/fulfillment, and cycle-count flows with barcode scanning and stock posting.
- [x] Implement mobile MRO picking and approved-return verification/posting.
- [x] Implement assigned-picker STO picking with item/source-location scans and Goods Issue posting.

## Phase 7: ERP and advanced configuration

- [~] Define the provider-neutral ERP SPI and SAP/Oracle/Accurate provider keys; concrete connectors need product/API versions and field mappings.
- [x] Add configurable item/location barcode prefixes with backward compatibility for WMS1/SLOC1.
- [x] Add admin-configurable Delivery Order zone exclusions to available-stock reservation.
- [x] Apply configurable zone exclusions to manual/SLOC and barcode-based stock transfers.
- [x] Add configurable allowed picklist zones for Stock Transfer and Purchase Return.
- [ ] Validate live synchronization when vendor docs, credentials, and mappings are provided.
