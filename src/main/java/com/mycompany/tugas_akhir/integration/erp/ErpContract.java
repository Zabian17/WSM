package com.mycompany.tugas_akhir.integration.erp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ErpContract {
    public enum Provider { SAP, ORACLE, ACCURATE }
    public enum DocumentType { PURCHASE_ORDER, SALES_ORDER }
    public enum EventType { WAREHOUSE_STATUS, SALES_INVOICE, MATERIAL_USAGE }
    public enum PublishStatus { ACCEPTED, DUPLICATE, RETRYABLE_FAILURE, PERMANENT_FAILURE }

    public record SyncRequest(DocumentType type, String cursor, int pageSize) {
        public SyncRequest {
            Objects.requireNonNull(type, "Document type is required.");
            if (pageSize < 1 || pageSize > 500) throw new IllegalArgumentException("Page size must be 1-500.");
            if (cursor != null && cursor.length() > 2048) throw new IllegalArgumentException("Cursor is too long.");
        }
    }

    public record DocumentLine(String itemCode, BigDecimal quantity, String unit) {
        public DocumentLine {
            requireText(itemCode, "Item code");
            Objects.requireNonNull(quantity, "Quantity is required.");
            requireText(unit, "Unit");
            if (quantity.signum() <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        }
    }

    public record Document(String externalId, String documentNumber, DocumentType type,
                           Instant changedAt, List<DocumentLine> lines) {
        public Document {
            requireText(externalId, "External document ID");
            requireText(documentNumber, "Document number");
            Objects.requireNonNull(type, "Document type is required.");
            Objects.requireNonNull(changedAt, "Change time is required.");
            lines = List.copyOf(Objects.requireNonNull(lines, "Document lines are required."));
        }
    }

    public record DocumentPage(List<Document> documents, String nextCursor) {
        public DocumentPage {
            documents = List.copyOf(Objects.requireNonNull(documents, "Documents are required."));
            if (nextCursor != null && nextCursor.length() > 2048) throw new IllegalArgumentException("Cursor is too long.");
        }
    }

    public record EventLine(String itemCode, String locationCode, String lotCode,
                            BigDecimal quantity, String unit) {
        public EventLine {
            requireText(itemCode, "Item code");
            requireText(locationCode, "Location code");
            Objects.requireNonNull(quantity, "Quantity is required.");
            requireText(unit, "Unit");
            if (quantity.signum() < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    public record WarehouseEvent(String eventId, EventType type, String externalDocumentId,
                                 Instant occurredAt, List<EventLine> lines) {
        public WarehouseEvent {
            requireText(eventId, "Event ID");
            Objects.requireNonNull(type, "Event type is required.");
            requireText(externalDocumentId, "External document ID");
            Objects.requireNonNull(occurredAt, "Event time is required.");
            lines = List.copyOf(Objects.requireNonNull(lines, "Event lines are required."));
            if (type != EventType.WAREHOUSE_STATUS
                    && lines.stream().anyMatch(line -> line.quantity().signum() == 0)) {
                throw new IllegalArgumentException("Movement event quantities must be positive.");
            }
        }
    }

    public record PublishResult(PublishStatus status, String externalReference, String message) {
        public PublishResult {
            Objects.requireNonNull(status, "Publish status is required.");
        }
    }

    private ErpContract() {}

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
    }
}
