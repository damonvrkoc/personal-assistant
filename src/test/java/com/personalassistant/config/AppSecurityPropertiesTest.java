package com.personalassistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.personalassistant.channel.ChannelType;
import org.junit.jupiter.api.Test;

class AppSecurityPropertiesTest {

    @Test
    void allowsAllWhenAllowlistEmpty() {
        var props = new AppSecurityProperties();
        assertThat(props.isAllowed(ChannelType.SLACK, "U123")).isTrue();
        assertThat(props.isAllowed(ChannelType.WHATSAPP, "1555")).isTrue();
    }

    @Test
    void enforcesPerChannelAllowlist() {
        var props = new AppSecurityProperties();
        props.setAllowedSlackUserIdsCsv("U1,U2");
        props.setAllowedWaIdsCsv("1555");

        assertThat(props.isAllowed(ChannelType.SLACK, "U1")).isTrue();
        assertThat(props.isAllowed(ChannelType.SLACK, "U9")).isFalse();
        assertThat(props.isAllowed(ChannelType.WHATSAPP, "1555")).isTrue();
        assertThat(props.isAllowed(ChannelType.WHATSAPP, "9999")).isFalse();
    }
}
