package com.personalassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp.cloud")
public record WhatsAppCloudProperties(
        String verifyToken,
        String appSecret,
        String accessToken,
        String phoneNumberId,
        String apiVersion,
        String graphBaseUrl) {

    public WhatsAppCloudProperties {
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = "v21.0";
        }
        if (graphBaseUrl == null || graphBaseUrl.isBlank()) {
            graphBaseUrl = "https://graph.facebook.com";
        }
        if (verifyToken == null) {
            verifyToken = "";
        }
        if (appSecret == null) {
            appSecret = "";
        }
        if (accessToken == null) {
            accessToken = "";
        }
        if (phoneNumberId == null) {
            phoneNumberId = "";
        }
    }
}
