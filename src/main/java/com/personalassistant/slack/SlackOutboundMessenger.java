package com.personalassistant.slack;

import com.personalassistant.channel.ChannelType;
import com.personalassistant.channel.OutboundMessenger;
import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.SlackProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.SLACK_ENABLED, havingValue = "true")
public class SlackOutboundMessenger implements OutboundMessenger {

    private final SlackApiClient client;
    private final SlackProperties properties;

    public SlackOutboundMessenger(SlackApiClient client, SlackProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.SLACK;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public void sendText(String channelId, String text) {
        client.postMessage(channelId, text);
    }
}
