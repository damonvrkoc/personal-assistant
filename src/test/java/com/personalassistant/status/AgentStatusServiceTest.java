package com.personalassistant.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.personalassistant.config.ChannelProperties;
import com.personalassistant.config.SlackProperties;
import com.personalassistant.config.WhatsAppCloudProperties;
import com.personalassistant.knowledge.Neo4jPersistenceState;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.slack.SlackConnectionState;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

class AgentStatusServiceTest {

    @Test
    void buildsStatusSnapshotWhenDriverMissing() {
        var channelProperties = new ChannelProperties(new ChannelProperties.ChannelToggle(false), new ChannelProperties.ChannelToggle(true));
        var slackProperties = new SlackProperties("bot", "app", true, "U1");
        var whatsAppProperties = new WhatsAppCloudProperties("", "", "", "", "v21.0", "https://graph.facebook.com");
        var slackState = new SlackConnectionState();
        slackState.setStatus(SlackConnectionState.Status.CONNECTED);
        var memory = mock(ConversationMemoryService.class);
        org.mockito.Mockito.when(memory.activeConversationCount()).thenReturn(3);

        var env = new MockEnvironment()
                .withProperty("spring.neo4j.uri", "bolt://localhost:7687")
                .withProperty("spring.neo4j.authentication.username", "neo4j")
                .withProperty("spring.neo4j.authentication.password", "secret")
                .withProperty("spring.ai.openai.chat.options.model", "gpt-4o-mini")
                .withProperty("spring.ai.openai.base-url", "https://api.openai.com")
                .withProperty("spring.ai.openai.api-key", "sk-test");

        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator> healthProvider =
                mock(ObjectProvider.class);
        org.mockito.Mockito.when(healthProvider.getIfAvailable()).thenReturn(null);

        var persistenceState = new Neo4jPersistenceState();
        persistenceState.recordFailure("Auth failed");

        var service = new AgentStatusService(
                channelProperties,
                slackProperties,
                whatsAppProperties,
                slackState,
                memory,
                new ApplicationUptime(),
                env,
                Optional.empty(),
                healthProvider,
                persistenceState,
                "personal-assistant",
                "0.1.0-SNAPSHOT");

        AgentStatusResponse status = service.getStatus();

        assertThat(status.neo4j().configured()).isTrue();
        assertThat(status.neo4j().status()).isEqualTo("DOWN");
        assertThat(status.neo4j().uri()).isEqualTo("bolt://localhost:7687");
        assertThat(status.neo4j().detail()).contains("Driver bean");
        assertThat(status.neo4j().lastPersistence()).isEqualTo("FAILED");
    }

    @Test
    void reportsNotConfiguredWhenUriMissing() {
        var env = new MockEnvironment().withProperty("spring.neo4j.authentication.username", "neo4j");

        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator> healthProvider =
                mock(ObjectProvider.class);

        var service = new AgentStatusService(
                new ChannelProperties(new ChannelProperties.ChannelToggle(false), new ChannelProperties.ChannelToggle(true)),
                new SlackProperties("", "", true, "U1"),
                new WhatsAppCloudProperties("", "", "", "", "v21.0", "https://graph.facebook.com"),
                new SlackConnectionState(),
                mock(ConversationMemoryService.class),
                new ApplicationUptime(),
                env,
                Optional.empty(),
                healthProvider,
                new Neo4jPersistenceState(),
                "personal-assistant",
                "0.1.0-SNAPSHOT");

        assertThat(service.getStatus().neo4j().configured()).isFalse();
        assertThat(service.getStatus().neo4j().detail()).contains("not configured");
    }
}
