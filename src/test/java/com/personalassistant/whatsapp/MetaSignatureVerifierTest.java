package com.personalassistant.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MetaSignatureVerifierTest {

    private final MetaSignatureVerifier verifier = new MetaSignatureVerifier();

    @Test
    void acceptsValidSignature() {
        String secret = "my_app_secret";
        byte[] body = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        String header = "sha256=" + hmacHex(secret, body);
        assertThat(verifier.isValid(body, header, secret)).isTrue();
    }

    @Test
    void rejectsTamperedBody() {
        String secret = "my_app_secret";
        byte[] body = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        String header = "sha256=" + hmacHex(secret, body);
        byte[] tampered = "{\"hello\":\"mars\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(tampered, header, secret)).isFalse();
    }

    @Test
    void rejectsMissingPrefix() {
        String secret = "my_app_secret";
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(body, "deadbeef", secret)).isFalse();
    }

    private static String hmacHex(String secret, byte[] body) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
