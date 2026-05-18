package com.personalassistant.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.personalassistant.config.ChannelProperties;
import com.personalassistant.config.SlackProperties;
import com.personalassistant.config.WhatsAppCloudProperties;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.slack.SlackConnectionState;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AgentStatusServiceTest {

    @Test
    void buildsStatusSnapshot() {
        var channelProperties = new ChannelProperties(new ChannelProperties.ChannelToggle(false), new ChannelProperties.ChannelToggle(true));
        var slackProperties = new SlackProperties("bot", "app", true, "U1");
        var whatsAppProperties = new WhatsAppCloudProperties("", "", "", "", "v21.0", "https://graph.facebook.com");
        var slackState = new SlackConnectionState();
        slackState.setStatus(SlackConnectionState.Status.CONNECTED);
        var memory = mock(ConversationMemoryService.class);
        org.mockito.Mockito.when(memory.activeConversationCount()).thenReturn(3);

        var env = new MockEnvironment()
                .withProperty("spring.ai.openai.chat.options.model", "gpt-4o-mini")
                .withProperty("spring.ai.openai.base-url", "https://api.openai.com")
                .withProperty("spring.ai.openai.api-key", "sk-test");

        var service = new AgentStatusService(
                channelProperties,
                slackProperties,
                whatsAppProperties,
                slackState,
                memory,
                new ApplicationUptime(),
                env,
                Optional.empty(),
                "personal-assistant",
                "0.1.0-SNAPSHOT");

        AgentStatusResponse status = service.getStatus();

        assertThat(status.channels().slack().socketMode()).isEqualTo("CONNECTED");
        assertThat(status.channels().whatsapp().enabled()).isFalse();
        assertThat(status.llm().apiKeyConfigured()).isTrue();
        assertThat(status.neo4j().status()).isEqualTo("DOWN");
        assertThat(status.memory().activeConversations()).isEqualTo(3);
    }
}
