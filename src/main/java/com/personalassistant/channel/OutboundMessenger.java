package com.personalassistant.channel;

public interface OutboundMessenger {

    ChannelType channel();

    boolean isConfigured();

    void sendText(String userId, String text);
}
