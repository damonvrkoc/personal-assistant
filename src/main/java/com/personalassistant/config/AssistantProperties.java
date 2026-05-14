package com.personalassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(String systemPrompt, int maxHistoryMessages) {

    public AssistantProperties {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a helpful personal assistant.";
        }
        if (maxHistoryMessages < 1) {
            maxHistoryMessages = 40;
        }
    }
}
