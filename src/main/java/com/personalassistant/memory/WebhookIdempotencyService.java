package com.personalassistant.memory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personalassistant.config.MemoryProperties;
import org.springframework.stereotype.Service;

@Service
public class WebhookIdempotencyService {

    private final Cache<String, Boolean> seen;

    public WebhookIdempotencyService(MemoryProperties memoryProperties) {
        this.seen = Caffeine.newBuilder()
                .expireAfterWrite(memoryProperties.idempotency().ttl())
                .maximumSize(500_000)
                .build();
    }

    /** @return true if this is the first time we see {@code wamid} */
    public boolean claim(String wamid) {
        if (wamid == null || wamid.isBlank()) {
            return false;
        }
        return seen.asMap().putIfAbsent(wamid, Boolean.TRUE) == null;
    }

    public void forget(String wamid) {
        if (wamid != null && !wamid.isBlank()) {
            seen.invalidate(wamid);
        }
    }
}
