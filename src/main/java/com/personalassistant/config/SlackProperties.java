package com.personalassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String botToken,
        String appToken,
        boolean startupNotifyEnabled,
        String startupNotifyUser) {

    public SlackProperties {
        if (botToken == null) {
            botToken = "";
        }
        if (appToken == null) {
            appToken = "";
        }
        if (startupNotifyUser == null || startupNotifyUser.isBlank()) {
            startupNotifyUser = "damon.vrkoc";
        }
    }

    public boolean isConfigured() {
        return !botToken.isBlank() && !appToken.isBlank();
    }
}
