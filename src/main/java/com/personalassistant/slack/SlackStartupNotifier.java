package com.personalassistant.slack;

import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.ChannelProperties;
import com.personalassistant.config.SlackProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.SLACK_ENABLED, havingValue = "true")
public class SlackStartupNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackStartupNotifier.class);

    private final SlackProperties slackProperties;
    private final ChannelProperties channelProperties;
    private final SlackApiClient slackApiClient;
    private final SlackUserResolver slackUserResolver;
    private final Environment environment;

    public SlackStartupNotifier(
            SlackProperties slackProperties,
            ChannelProperties channelProperties,
            SlackApiClient slackApiClient,
            SlackUserResolver slackUserResolver,
            Environment environment) {
        this.slackProperties = slackProperties;
        this.channelProperties = channelProperties;
        this.slackApiClient = slackApiClient;
        this.slackUserResolver = slackUserResolver;
        this.environment = environment;
    }

    public void sendStartupRecap() {
        if (!slackProperties.startupNotifyEnabled()) {
            log.debug("Slack startup notify disabled");
            return;
        }
        if (!slackProperties.isConfigured()) {
            return;
        }

        String userRef = slackProperties.startupNotifyUser();
        var userId = slackUserResolver.resolveUserId(userRef);
        if (userId.isEmpty()) {
            log.warn(
                    "Could not resolve Slack user '{}'. Set SLACK_STARTUP_NOTIFY_USER to a member ID (U...) "
                            + "and ensure the bot has users:read scope.",
                    userRef);
            return;
        }

        try {
            slackApiClient.postDirectMessage(userId.get(), buildRecapMessage());
            log.info("Sent startup recap DM to Slack user {} ({})", userRef, userId.get());
        } catch (RuntimeException e) {
            log.warn("Failed to send startup recap to Slack user {}", userRef, e);
        }
    }

    private String buildRecapMessage() {
        String model = environment.getProperty("spring.ai.openai.chat.options.model", "gpt-4o-mini");
        boolean whatsappOn = channelProperties.whatsapp().enabled();
        boolean slackOn = channelProperties.slack().enabled();

        return """
                *Personal assistant is online*

                Short recap of what works right now:
                • *Slack DMs* — Socket Mode is connected; reply in this DM to chat with me
                • *ChatGPT / OpenAI* — simple conversation via Spring AI (`%s`)
                • *Memory* — recent turns kept in RAM for this thread
                • *Neo4j* — conversation turns saved asynchronously (if DB is up)

                Channel status: Slack=%s, WhatsApp=%s

                _Send "Hello" here to verify the chat flow end-to-end._
                """
                .formatted(model, onOff(slackOn), onOff(whatsappOn))
                .strip();
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }
}
