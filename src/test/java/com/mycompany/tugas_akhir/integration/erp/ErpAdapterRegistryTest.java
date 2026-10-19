package com.mycompany.tugas_akhir.integration.erp;

import com.mycompany.tugas_akhir.integration.erp.ErpContract.Provider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ErpAdapterRegistryTest {

    @Test
    void resolvesAdapterByProvider() {
        ErpAdapter adapter = adapter(Provider.SAP);

        assertEquals(Optional.of(adapter), new ErpAdapterRegistry(List.of(adapter)).find(Provider.SAP));
        assertTrue(new ErpAdapterRegistry(List.of(adapter)).find(Provider.ORACLE).isEmpty());
    }

    @Test
    void rejectsDuplicateAdaptersForOneProvider() {
        assertThrows(IllegalStateException.class,
                () -> new ErpAdapterRegistry(List.of(adapter(Provider.ACCURATE), adapter(Provider.ACCURATE))));
    }

    @Test
    void syncPageSizeMustBeBounded() {
        assertThrows(IllegalArgumentException.class,
                () -> new ErpContract.SyncRequest(ErpContract.DocumentType.PURCHASE_ORDER, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ErpContract.SyncRequest(ErpContract.DocumentType.PURCHASE_ORDER, null, 501));
    }

    @Test
    void normalizedDocumentLinesRejectInvalidQuantities() {
        assertThrows(IllegalArgumentException.class,
                () -> new ErpContract.DocumentLine("SKU-1", BigDecimal.ZERO, "pcs"));
        assertThrows(IllegalArgumentException.class,
                () -> new ErpContract.DocumentLine("SKU-1", BigDecimal.ONE.negate(), "pcs"));
    }

    private ErpAdapter adapter(Provider provider) {
        return new ErpAdapter() {
            @Override public Provider provider() { return provider; }
            @Override public ErpContract.DocumentPage fetchDocuments(ErpContract.SyncRequest request) {
                return new ErpContract.DocumentPage(List.of(), null);
            }
            @Override public ErpContract.PublishResult publish(ErpContract.WarehouseEvent event) {
                return new ErpContract.PublishResult(ErpContract.PublishStatus.ACCEPTED, null, null);
            }
        };
    }
}
