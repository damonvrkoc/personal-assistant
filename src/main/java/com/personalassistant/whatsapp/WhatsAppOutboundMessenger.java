package com.personalassistant.whatsapp;

import com.personalassistant.channel.ChannelType;
import com.personalassistant.channel.OutboundMessenger;
import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.WhatsAppCloudProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.WHATSAPP_ENABLED, havingValue = "true")
public class WhatsAppOutboundMessenger implements OutboundMessenger {

    private final WhatsAppCloudClient client;
    private final WhatsAppCloudProperties properties;

    public WhatsAppOutboundMessenger(WhatsAppCloudClient client, WhatsAppCloudProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.WHATSAPP;
    }

    @Override
    public boolean isConfigured() {
        return properties.accessToken() != null && !properties.accessToken().isBlank();
    }

    @Override
    public void sendText(String userId, String text) {
        client.sendTextMessage(userId, text);
    }
}
