package com.personalassistant.slack;

import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.SlackProperties;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.SLACK_ENABLED, havingValue = "true")
public class SlackApiClient {

    private final SlackProperties properties;
    private final Slack slack = Slack.getInstance();

    public SlackApiClient(SlackProperties properties) {
        this.properties = properties;
    }

    public void postDirectMessage(String userId, String text) {
        try {
            var openResponse = slack.methods(properties.botToken())
                    .conversationsOpen(ConversationsOpenRequest.builder()
                            .users(List.of(userId))
                            .build());
            if (!openResponse.isOk() || openResponse.getChannel() == null) {
                throw new IllegalStateException(
                        "Slack conversations.open failed: " + openResponse.getError());
            }
            postMessage(openResponse.getChannel().getId(), text);
        } catch (IOException | SlackApiException e) {
            throw new IllegalStateException("Slack DM open failed", e);
        }
    }

    public void postMessage(String channelId, String text) {
        try {
            var response = slack.methods(properties.botToken())
                    .chatPostMessage(ChatPostMessageRequest.builder()
                            .channel(channelId)
                            .text(text)
                            .build());
            if (!response.isOk()) {
                throw new IllegalStateException("Slack chat.postMessage failed: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            throw new IllegalStateException("Slack chat.postMessage failed", e);
        }
    }
}
