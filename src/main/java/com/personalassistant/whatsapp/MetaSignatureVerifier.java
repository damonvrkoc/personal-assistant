package com.personalassistant.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.personalassistant.config.ChannelConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.WHATSAPP_ENABLED, havingValue = "true")
public class MetaSignatureVerifier {

    public boolean isValid(byte[] rawBody, String signatureHeader, String appSecret) {
        if (appSecret == null || appSecret.isBlank()) {
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String providedHex = signatureHeader.substring("sha256=".length()).trim();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody);
            String computed = toHex(digest);
            return constantTimeEquals(computed, providedHex);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }
}
