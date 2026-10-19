package com.mycompany.tugas_akhir.api.mobile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobileApiNetworkGuardTest {
    @Test
    void allowsLoopbackWithoutTls() {
        assertDoesNotThrow(() -> new MobileApiNetworkGuard("127.0.0.1", false)
                .afterSingletonsInstantiated());
    }

    @Test
    void rejectsLanBindingWithoutTls() {
        assertThrows(IllegalStateException.class, () -> new MobileApiNetworkGuard("0.0.0.0", false)
                .afterSingletonsInstantiated());
    }

    @Test
    void permitsLanBindingWhenTlsIsEnabled() {
        assertDoesNotThrow(() -> new MobileApiNetworkGuard("192.0.2.4", true)
                .afterSingletonsInstantiated());
    }
}
