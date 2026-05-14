package com.personalassistant.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /** Comma-separated WhatsApp IDs (digits, no +). Empty means allow all. */
    private String allowedWaIdsCsv = "";

    public String getAllowedWaIdsCsv() {
        return allowedWaIdsCsv;
    }

    public void setAllowedWaIdsCsv(String allowedWaIdsCsv) {
        this.allowedWaIdsCsv = allowedWaIdsCsv == null ? "" : allowedWaIdsCsv;
    }

    public boolean isAllowed(String waId) {
        if (allowedWaIdsCsv == null || allowedWaIdsCsv.isBlank()) {
            return true;
        }
        return Arrays.stream(allowedWaIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(waId::equals);
    }
}
