package com.mycompany.tugas_akhir.api.mobile;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class MobileApiNetworkGuard implements SmartInitializingSingleton {
    private final String bindAddress;
    private final boolean tlsEnabled;

    public MobileApiNetworkGuard(@Value("${server.address:127.0.0.1}") String bindAddress,
                                 @Value("${server.ssl.enabled:false}") boolean tlsEnabled) {
        this.bindAddress = bindAddress;
        this.tlsEnabled = tlsEnabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!isLoopback(bindAddress) && !tlsEnabled) {
            throw new IllegalStateException("Mobile API hanya boleh dibuka ke jaringan dengan TLS aktif.");
        }
    }

    private boolean isLoopback(String address) {
        if (address == null || address.isBlank()) return false;
        if ("0.0.0.0".equals(address) || "::".equals(address) || "*".equals(address)) return false;
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("Alamat bind Mobile API tidak valid.", exception);
        }
    }
}
