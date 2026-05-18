package com.personalassistant.status;

import com.personalassistant.config.ChannelProperties;
import com.personalassistant.config.SlackProperties;
import com.personalassistant.config.WhatsAppCloudProperties;
import com.personalassistant.knowledge.Neo4jPersistenceState;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.slack.SlackConnectionState;
import java.util.Optional;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AgentStatusService {

    private static final Logger log = LoggerFactory.getLogger(AgentStatusService.class);

    private final ChannelProperties channelProperties;
    private final SlackProperties slackProperties;
    private final WhatsAppCloudProperties whatsAppCloudProperties;
    private final SlackConnectionState slackConnectionState;
    private final ConversationMemoryService conversationMemory;
    private final ApplicationUptime applicationUptime;
    private final Environment environment;
    private final Optional<Driver> neo4jDriver;
    private final ObjectProvider<Neo4jHealthIndicator> neo4jHealthIndicator;
    private final Neo4jPersistenceState neo4jPersistenceState;
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
            ObjectProvider<Neo4jHealthIndicator> neo4jHealthIndicator,
            Neo4jPersistenceState neo4jPersistenceState,
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
        this.neo4jHealthIndicator = neo4jHealthIndicator;
        this.neo4jPersistenceState = neo4jPersistenceState;
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
        String uri = environment.getProperty("spring.neo4j.uri", "");
        String username = environment.getProperty("spring.neo4j.authentication.username", "");
        String password = environment.getProperty("spring.neo4j.authentication.password", "");
        boolean configured = uri != null && !uri.isBlank() && username != null && !username.isBlank();
        String lastPersistence = neo4jPersistenceState.getLastWriteStatus().name();

        if (!configured) {
            return new AgentStatusResponse.Neo4jInfo(
                    false, "DOWN", safeUri(uri), "Neo4j URI or username is not configured", lastPersistence);
        }
        if (password == null || password.isBlank()) {
            log.warn("Neo4j password is empty; connectivity check may fail (set NEO4J_PASSWORD in .env)");
        }

        ConnectivityCheck check = checkConnectivity();
        if ("UP".equals(check.status())) {
            return new AgentStatusResponse.Neo4jInfo(configured, "UP", safeUri(uri), check.detail(), lastPersistence);
        }

        log.warn("Neo4j connectivity check failed: {}", check.detail());
        String detail = appendPersistenceDetail(check.detail());
        return new AgentStatusResponse.Neo4jInfo(configured, "DOWN", safeUri(uri), detail, lastPersistence);
    }

    private ConnectivityCheck checkConnectivity() {
        Neo4jHealthIndicator indicator = neo4jHealthIndicator.getIfAvailable();
        if (indicator != null) {
            try {
                Health health = indicator.health();
                if (Status.UP.equals(health.getStatus())) {
                    return new ConnectivityCheck("UP", null);
                }
                String detail = formatHealthDetail(health);
                return new ConnectivityCheck("DOWN", detail);
            } catch (Exception e) {
                return new ConnectivityCheck("DOWN", sanitize(e));
            }
        }

        if (neo4jDriver.isEmpty()) {
            return new ConnectivityCheck("DOWN", "Neo4j Driver bean is not available");
        }

        try {
            neo4jDriver.get().verifyConnectivity();
            return new ConnectivityCheck("UP", null);
        } catch (Neo4jException e) {
            return new ConnectivityCheck("DOWN", sanitize(e));
        } catch (Exception e) {
            return new ConnectivityCheck("DOWN", sanitize(e));
        }
    }

    private static String formatHealthDetail(Health health) {
        Object error = health.getDetails().get("error");
        if (error != null) {
            return String.valueOf(error);
        }
        return health.getDetails().isEmpty() ? health.getStatus().getCode() : health.getDetails().toString();
    }

    private static String sanitize(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    private static String safeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        return uri.replaceAll("://[^@]+@", "://***@");
    }

    private String appendPersistenceDetail(String connectivityDetail) {
        String base = connectivityDetail != null ? connectivityDetail : "Unknown error";
        return neo4jPersistenceState
                .getLastFailureDetail()
                .map(persistDetail -> base + "; last write: " + persistDetail)
                .orElse(base);
    }

    private record ConnectivityCheck(String status, String detail) {}

}
