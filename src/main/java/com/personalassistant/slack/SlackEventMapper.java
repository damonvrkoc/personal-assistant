package com.personalassistant.slack;

import com.personalassistant.channel.ChannelMessage;
import com.personalassistant.channel.ChannelType;
import com.slack.api.model.event.MessageEvent;
import java.util.Optional;

public final class SlackEventMapper {

    private SlackEventMapper() {}

    public static Optional<ChannelMessage> toChannelMessage(MessageEvent event, String eventId) {
        if (event == null) {
            return Optional.empty();
        }
        if (event.getBotId() != null || event.getBotProfile() != null) {
            return Optional.empty();
        }
        String subtype = event.getSubtype();
        if (subtype != null && !subtype.isBlank()) {
            return Optional.empty();
        }
        if (!"im".equals(event.getChannelType())) {
            return Optional.empty();
        }
        String text = event.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String userId = event.getUser();
        String channelId = event.getChannel();
        if (userId == null || userId.isBlank() || channelId == null || channelId.isBlank()) {
            return Optional.empty();
        }
        String messageId = eventId != null && !eventId.isBlank() ? eventId : event.getClientMsgId();
        if (messageId == null || messageId.isBlank()) {
            messageId = userId + ":" + event.getTs();
        }
        long ts = 0L;
        if (event.getEventTs() != null) {
            try {
                ts = (long) Double.parseDouble(event.getEventTs());
            } catch (NumberFormatException ignored) {
                // keep 0
            }
        }
        return Optional.of(new ChannelMessage(
                ChannelType.SLACK, userId, text, messageId, ts, channelId));
    }
}
