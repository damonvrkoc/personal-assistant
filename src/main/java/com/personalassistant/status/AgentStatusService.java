package com.personalassistant.status;

import com.personalassistant.config.ChannelProperties;
import com.personalassistant.config.SlackProperties;
import com.personalassistant.config.WhatsAppCloudProperties;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.slack.SlackConnectionState;
import java.util.Optional;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AgentStatusService {

    private final ChannelProperties channelProperties;
    private final SlackProperties slackProperties;
    private final WhatsAppCloudProperties whatsAppCloudProperties;
    private final SlackConnectionState slackConnectionState;
    private final ConversationMemoryService conversationMemory;
    private final ApplicationUptime applicationUptime;
    private final Environment environment;
    private final Optional<Driver> neo4jDriver;
    private final String applicationName;
    private final String applicationVersion;

    public AgentStatusService(
            ChannelProperties channelProperties,
            SlackProperties slackProperties,
            WhatsAppCloudProperties whatsAppCloudProperties,
            SlackConnectionState slackConnectionState,
            ConversationMemoryService conversationMemory,
            ApplicationUptime applicationUptime,
            Environment environment,
            Optional<Driver> neo4jDriver,
            @Value("${spring.application.name:personal-assistant}") String applicationName,
            @Value("${app.version:0.1.0-SNAPSHOT}") String applicationVersion) {
        this.channelProperties = channelProperties;
        this.slackProperties = slackProperties;
        this.whatsAppCloudProperties = whatsAppCloudProperties;
        this.slackConnectionState = slackConnectionState;
        this.conversationMemory = conversationMemory;
        this.applicationUptime = applicationUptime;
        this.environment = environment;
        this.neo4jDriver = neo4jDriver;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public AgentStatusResponse getStatus() {
        return new AgentStatusResponse(
                new AgentStatusResponse.ApplicationInfo(
                        applicationName, applicationVersion, applicationUptime.uptime().toString()),
                new AgentStatusResponse.ChannelsInfo(slackStatus(), whatsappStatus()),
                llmStatus(),
                neo4jStatus(),
                new AgentStatusResponse.MemoryInfo(conversationMemory.activeConversationCount()));
    }

    private AgentStatusResponse.ChannelStatus slackStatus() {
        boolean enabled = channelProperties.slack().enabled();
        boolean configured = slackProperties.isConfigured();
        String socketMode =
                switch (resolveSlackSocketMode()) {
                    case DISABLED -> "DISABLED";
                    case NOT_CONFIGURED -> "NOT_CONFIGURED";
                    case CONNECTED -> "CONNECTED";
                    case FAILED -> "FAILED";
                };
        return new AgentStatusResponse.ChannelStatus(enabled, configured, socketMode);
    }

    private SlackConnectionState.Status resolveSlackSocketMode() {
        if (!channelProperties.slack().enabled()) {
            return SlackConnectionState.Status.DISABLED;
        }
        return slackConnectionState.getStatus();
    }

    private AgentStatusResponse.ChannelStatus whatsappStatus() {
        boolean enabled = channelProperties.whatsapp().enabled();
        boolean configured = whatsAppCloudProperties.accessToken() != null
                && !whatsAppCloudProperties.accessToken().isBlank()
                && whatsAppCloudProperties.phoneNumberId() != null
                && !whatsAppCloudProperties.phoneNumberId().isBlank();
        return new AgentStatusResponse.ChannelStatus(enabled, configured, "N/A");
    }

    private AgentStatusResponse.LlmInfo llmStatus() {
        String model = environment.getProperty("spring.ai.openai.chat.options.model", "gpt-4o-mini");
        String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        boolean apiKeyConfigured = apiKey != null && !apiKey.isBlank() && !"dummy-local-key".equals(apiKey);
        return new AgentStatusResponse.LlmInfo("openai-compatible", model, baseUrl, apiKeyConfigured);
    }

    private AgentStatusResponse.Neo4jInfo neo4jStatus() {
        if (neo4jDriver.isEmpty()) {
            return new AgentStatusResponse.Neo4jInfo("DOWN");
        }
        try {
            neo4jDriver.get().verifyConnectivity();
            return new AgentStatusResponse.Neo4jInfo("UP");
        } catch (Neo4jException e) {
            return new AgentStatusResponse.Neo4jInfo("DOWN");
        }
    }
}
