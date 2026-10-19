package com.mycompany.tugas_akhir.integration.erp;

import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;

final class AccurateApiTokenSigner {
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private AccurateApiTokenSigner() {}

    static void apply(HttpHeaders headers, String token, String secret, Clock clock) {
        String timestamp = Long.toString(clock.millis());
        headers.setBearerAuth(token);
        headers.set("X-Api-Timestamp", timestamp);
        headers.set("X-Api-Signature", sign(secret, timestamp));
    }

    static String sign(String secret, String timestamp) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign Accurate API Token request.", exception);
        }
    }
}
