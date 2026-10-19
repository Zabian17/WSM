package com.mycompany.tugas_akhir.integration.erp;

import com.mycompany.tugas_akhir.integration.erp.ErpContract.DocumentPage;
import com.mycompany.tugas_akhir.integration.erp.ErpContract.PublishResult;
import com.mycompany.tugas_akhir.integration.erp.ErpContract.Provider;
import com.mycompany.tugas_akhir.integration.erp.ErpContract.SyncRequest;
import com.mycompany.tugas_akhir.integration.erp.ErpContract.WarehouseEvent;

/** Maps one ERP provider's API to the provider-neutral WMS document and event contract. */
public interface ErpAdapter {
    Provider provider();

    /** Fetches one bounded page of normalized PO or SO documents using an opaque provider cursor. */
    DocumentPage fetchDocuments(SyncRequest request);

    /** Publishes one event using its stable event ID as the retry/idempotency key. */
    PublishResult publish(WarehouseEvent event);
}
