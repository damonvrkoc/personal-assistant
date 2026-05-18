package com.personalassistant.channel;

public record ChannelMessage(
        ChannelType channel,
        String userId,
        String text,
        String messageId,
        long timestampSeconds,
        String replyTarget) {

    public ChannelMessage {
        if (replyTarget == null || replyTarget.isBlank()) {
            replyTarget = userId;
        }
    }

    public String conversationKey() {
        return channel.name().toLowerCase() + ":" + userId;
    }

    public String outboundTarget() {
        return replyTarget;
    }
}
