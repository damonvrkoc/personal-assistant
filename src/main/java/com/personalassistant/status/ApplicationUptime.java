package com.personalassistant.status;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ApplicationUptime {

    private final Instant startedAt = Instant.now();

    public Duration uptime() {
        return Duration.between(startedAt, Instant.now());
    }
}
