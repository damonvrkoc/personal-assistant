package com.personalassistant.assistant;

import com.personalassistant.channel.ChannelMessage;
import com.personalassistant.channel.ChannelType;
import com.personalassistant.channel.OutboundMessenger;
import com.personalassistant.config.AppSecurityProperties;
import com.personalassistant.config.AssistantProperties;
import com.personalassistant.knowledge.TurnPersistenceService;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.memory.WebhookIdempotencyService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatModel chatModel;
    private final ConversationMemoryService conversationMemory;
    private final WebhookIdempotencyService idempotency;
    private final AppSecurityProperties securityProperties;
    private final AssistantProperties assistantProperties;
    private final Map<ChannelType, OutboundMessenger> outboundByChannel;
    private final TurnPersistenceService turnPersistenceService;

    public AssistantService(
            ChatModel chatModel,
            ConversationMemoryService conversationMemory,
            WebhookIdempotencyService idempotency,
            AppSecurityProperties securityProperties,
            AssistantProperties assistantProperties,
            List<OutboundMessenger> outboundMessengers,
            TurnPersistenceService turnPersistenceService) {
        this.chatModel = chatModel;
        this.conversationMemory = conversationMemory;
        this.idempotency = idempotency;
        this.securityProperties = securityProperties;
        this.assistantProperties = assistantProperties;
        this.outboundByChannel = outboundMessengers.stream()
                .collect(Collectors.toMap(OutboundMessenger::channel, Function.identity()));
        this.turnPersistenceService = turnPersistenceService;
    }

    public void handle(ChannelMessage message) {
        if (!securityProperties.isAllowed(message.channel(), message.userId())) {
            log.debug(
                    "Ignoring message from non-allowlisted user channel={} userId={}",
                    message.channel(),
                    message.userId());
            return;
        }
        if (!idempotency.claim(message.messageId())) {
            log.debug("Duplicate message messageId={}", message.messageId());
            return;
        }

        OutboundMessenger outbound = outboundByChannel.get(message.channel());
        if (outbound == null) {
            log.warn("No outbound messenger registered for channel={}", message.channel());
            idempotency.forget(message.messageId());
            return;
        }
        if (!outbound.isConfigured()) {
            log.warn("Outbound channel {} is not configured; skipping reply", message.channel());
            idempotency.forget(message.messageId());
            return;
        }

        String conversationKey = message.conversationKey();
        try {
            conversationMemory.appendUser(conversationKey, message.text());
            var prompt = new Prompt(conversationMemory.buildModelMessages(conversationKey, assistantProperties));
            var response = chatModel.call(prompt);
            String replyText = response.getResult().getOutput().getText();
            conversationMemory.appendAssistant(conversationKey, replyText);

            turnPersistenceService.recordTurnAsync(message.channel(), message.userId(), "user", message.text());
            turnPersistenceService.recordTurnAsync(message.channel(), message.userId(), "assistant", replyText);

            outbound.sendText(message.outboundTarget(), replyText);
        } catch (RuntimeException e) {
            idempotency.forget(message.messageId());
            throw e;
        }
    }
}
