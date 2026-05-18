package com.personalassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels")
public record ChannelProperties(ChannelToggle whatsapp, ChannelToggle slack) {

    public ChannelProperties {
        if (whatsapp == null) {
            whatsapp = new ChannelToggle(false);
        }
        if (slack == null) {
            slack = new ChannelToggle(true);
        }
    }

    public record ChannelToggle(boolean enabled) {}
}
