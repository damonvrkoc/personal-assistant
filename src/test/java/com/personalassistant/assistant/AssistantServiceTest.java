package com.personalassistant.assistant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personalassistant.channel.ChannelMessage;
import com.personalassistant.channel.ChannelType;
import com.personalassistant.channel.OutboundMessenger;
import com.personalassistant.config.AppSecurityProperties;
import com.personalassistant.config.AssistantProperties;
import com.personalassistant.knowledge.TurnPersistenceService;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.memory.WebhookIdempotencyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ConversationMemoryService conversationMemory;

    @Mock
    private WebhookIdempotencyService idempotency;

    @Mock
    private TurnPersistenceService turnPersistenceService;

    @Mock
    private OutboundMessenger slackMessenger;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        when(slackMessenger.channel()).thenReturn(ChannelType.SLACK);
        assistantService = new AssistantService(
                chatModel,
                conversationMemory,
                idempotency,
                new AppSecurityProperties(),
                new AssistantProperties("You are helpful.", 40),
                List.of(slackMessenger),
                turnPersistenceService);
    }

    @Test
    void handlesMessageAndRepliesViaOutboundMessenger() {
        ChannelMessage message =
                new ChannelMessage(ChannelType.SLACK, "U1", "hi", "msg-1", 0L, "D1");
        when(slackMessenger.isConfigured()).thenReturn(true);
        when(idempotency.claim("msg-1")).thenReturn(true);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("hello back")))));

        assistantService.handle(message);

        verify(conversationMemory).appendUser("slack:U1", "hi");
        verify(slackMessenger).sendText("D1", "hello back");
        verify(turnPersistenceService).recordTurnAsync(ChannelType.SLACK, "U1", "user", "hi");
    }

    @Test
    void skipsDuplicateMessages() {
        ChannelMessage message =
                new ChannelMessage(ChannelType.SLACK, "U1", "hi", "msg-1", 0L, "D1");
        when(idempotency.claim("msg-1")).thenReturn(false);

        assistantService.handle(message);

        verify(chatModel, never()).call(any(Prompt.class));
        verify(slackMessenger, never()).sendText(any(), any());
    }
}
