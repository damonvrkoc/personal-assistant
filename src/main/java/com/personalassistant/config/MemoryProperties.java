package com.personalassistant.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "memory")
public record MemoryProperties(Conversation conversation, Idempotency idempotency) {

    public MemoryProperties {
        if (conversation == null) {
            conversation = new Conversation(Duration.ofHours(24), 80);
        }
        if (idempotency == null) {
            idempotency = new Idempotency(Duration.ofHours(48));
        }
    }

    public record Conversation(Duration ttl, int maxMessagesPerUser) {}

    public record Idempotency(Duration ttl) {}
}
