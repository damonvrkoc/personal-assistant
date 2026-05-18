package com.personalassistant.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.personalassistant.channel.ChannelType;
import com.slack.api.model.event.MessageEvent;
import org.junit.jupiter.api.Test;

class SlackEventMapperTest {

    @Test
    void mapsDirectMessage() {
        MessageEvent event = new MessageEvent();
        event.setUser("U123");
        event.setChannel("D456");
        event.setChannelType("im");
        event.setText("Hello");

        var message = SlackEventMapper.toChannelMessage(event, "Ev123").orElseThrow();
        assertThat(message.channel()).isEqualTo(ChannelType.SLACK);
        assertThat(message.userId()).isEqualTo("U123");
        assertThat(message.outboundTarget()).isEqualTo("D456");
        assertThat(message.conversationKey()).isEqualTo("slack:U123");
        assertThat(message.messageId()).isEqualTo("Ev123");
    }

    @Test
    void ignoresBotMessages() {
        MessageEvent event = new MessageEvent();
        event.setUser("U123");
        event.setChannel("D456");
        event.setChannelType("im");
        event.setText("Hello");
        event.setBotId("B999");

        assertThat(SlackEventMapper.toChannelMessage(event, "Ev123")).isEmpty();
    }

    @Test
    void ignoresNonDmChannels() {
        MessageEvent event = new MessageEvent();
        event.setUser("U123");
        event.setChannel("C456");
        event.setChannelType("channel");
        event.setText("Hello");

        assertThat(SlackEventMapper.toChannelMessage(event, "Ev123")).isEmpty();
    }
}
