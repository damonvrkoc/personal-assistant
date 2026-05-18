package com.personalassistant.config;

import com.personalassistant.channel.ChannelType;
import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /** Comma-separated WhatsApp IDs (digits, no +). Empty means allow all. */
    private String allowedWaIdsCsv = "";

    /** Comma-separated Slack user IDs (e.g. U012ABC). Empty means allow all. */
    private String allowedSlackUserIdsCsv = "";

    public String getAllowedWaIdsCsv() {
        return allowedWaIdsCsv;
    }

    public void setAllowedWaIdsCsv(String allowedWaIdsCsv) {
        this.allowedWaIdsCsv = allowedWaIdsCsv == null ? "" : allowedWaIdsCsv;
    }

    public String getAllowedSlackUserIdsCsv() {
        return allowedSlackUserIdsCsv;
    }

    public void setAllowedSlackUserIdsCsv(String allowedSlackUserIdsCsv) {
        this.allowedSlackUserIdsCsv = allowedSlackUserIdsCsv == null ? "" : allowedSlackUserIdsCsv;
    }

    public boolean isAllowed(ChannelType channel, String userId) {
        String csv =
                switch (channel) {
                    case WHATSAPP -> allowedWaIdsCsv;
                    case SLACK -> allowedSlackUserIdsCsv;
                };
        if (csv == null || csv.isBlank()) {
            return true;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(userId::equals);
    }
}
