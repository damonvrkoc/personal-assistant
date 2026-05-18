package com.personalassistant.slack;

import com.personalassistant.assistant.AssistantService;
import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.SlackProperties;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.SLACK_ENABLED, havingValue = "true")
public class SlackSocketModeRunner
        implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SlackSocketModeRunner.class);

    private final SlackProperties slackProperties;
    private final AssistantService assistantService;
    private final SlackStartupNotifier slackStartupNotifier;
    private SocketModeApp socketModeApp;

    public SlackSocketModeRunner(
            SlackProperties slackProperties,
            AssistantService assistantService,
            SlackStartupNotifier slackStartupNotifier) {
        this.slackProperties = slackProperties;
        this.assistantService = assistantService;
        this.slackStartupNotifier = slackStartupNotifier;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!slackProperties.isConfigured()) {
            log.warn("Slack channel is enabled but SLACK_BOT_TOKEN / SLACK_APP_TOKEN are not set; Socket Mode will not start");
            return;
        }

        App app = new App();
        app.event(MessageEvent.class, (payload, ctx) -> {
            SlackEventMapper.toChannelMessage(payload.getEvent(), payload.getEventId())
                    .ifPresent(assistantService::handle);
            return ctx.ack();
        });

        try {
            socketModeApp = new SocketModeApp(slackProperties.appToken(), app);
            socketModeApp.startAsync();
            log.info("Slack Socket Mode connected");
            slackStartupNotifier.sendStartupRecap();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Slack Socket Mode", e);
        }
    }

    @Override
    public void destroy() {
        if (socketModeApp != null) {
            try {
                socketModeApp.close();
            } catch (Exception e) {
                log.warn("Error closing Slack Socket Mode", e);
            }
        }
    }
}
