package com.mycompany.tugas_akhir.integration.erp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccurateApiTokenSignerTest {
    @Test
    void signsTimestampUsingHmacSha256Hex() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                AccurateApiTokenSigner.sign("key", "The quick brown fox jumps over the lazy dog"));
    }
}
