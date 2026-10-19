package com.mycompany.tugas_akhir.integration.erp;

import com.mycompany.tugas_akhir.integration.erp.ErpContract.Provider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ErpAdapterRegistry {
    private final Map<Provider, ErpAdapter> adapters;

    public ErpAdapterRegistry(List<ErpAdapter> adapters) {
        Map<Provider, ErpAdapter> byProvider = new EnumMap<>(Provider.class);
        for (ErpAdapter adapter : adapters) {
            Provider provider = adapter.provider();
            if (provider == null || byProvider.putIfAbsent(provider, adapter) != null) {
                throw new IllegalStateException("ERP provider adapter is missing or duplicated: " + provider);
            }
        }
        this.adapters = Map.copyOf(byProvider);
    }

    public Optional<ErpAdapter> find(Provider provider) {
        return Optional.ofNullable(provider == null ? null : adapters.get(provider));
    }
}
