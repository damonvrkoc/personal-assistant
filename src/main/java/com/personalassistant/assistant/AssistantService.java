package com.personalassistant.assistant;

import com.personalassistant.config.AppSecurityProperties;
import com.personalassistant.config.AssistantProperties;
import com.personalassistant.config.WhatsAppCloudProperties;
import com.personalassistant.knowledge.TurnPersistenceService;
import com.personalassistant.memory.ConversationMemoryService;
import com.personalassistant.memory.WebhookIdempotencyService;
import com.personalassistant.whatsapp.InboundMessage;
import com.personalassistant.whatsapp.WhatsAppCloudClient;
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
    private final WhatsAppCloudClient whatsAppCloudClient;
    private final WhatsAppCloudProperties whatsAppCloudProperties;
    private final TurnPersistenceService turnPersistenceService;

    public AssistantService(
            ChatModel chatModel,
            ConversationMemoryService conversationMemory,
            WebhookIdempotencyService idempotency,
            AppSecurityProperties securityProperties,
            AssistantProperties assistantProperties,
            WhatsAppCloudClient whatsAppCloudClient,
            WhatsAppCloudProperties whatsAppCloudProperties,
            TurnPersistenceService turnPersistenceService) {
        this.chatModel = chatModel;
        this.conversationMemory = conversationMemory;
        this.idempotency = idempotency;
        this.securityProperties = securityProperties;
        this.assistantProperties = assistantProperties;
        this.whatsAppCloudClient = whatsAppCloudClient;
        this.whatsAppCloudProperties = whatsAppCloudProperties;
        this.turnPersistenceService = turnPersistenceService;
    }

    public void handle(InboundMessage message) {
        if (!securityProperties.isAllowed(message.waId())) {
            log.debug("Ignoring message from non-allowlisted wa_id={}", message.waId());
            return;
        }
        if (!idempotency.claim(message.messageId())) {
            log.debug("Duplicate webhook messageId={}", message.messageId());
            return;
        }
        if (whatsAppCloudProperties.accessToken() == null
                || whatsAppCloudProperties.accessToken().isBlank()) {
            log.warn("WHATSAPP_CLOUD_ACCESS_TOKEN is not configured; skipping outbound reply");
            idempotency.forget(message.messageId());
            return;
        }

        try {
            conversationMemory.appendUser(message.waId(), message.text());
            var prompt = new Prompt(conversationMemory.buildModelMessages(message.waId(), assistantProperties));
            var response = chatModel.call(prompt);
            String replyText = response.getResult().getOutput().getText();
            conversationMemory.appendAssistant(message.waId(), replyText);

            turnPersistenceService.recordTurnAsync(message.waId(), "user", message.text());
            turnPersistenceService.recordTurnAsync(message.waId(), "assistant", replyText);

            whatsAppCloudClient.sendTextMessage(message.waId(), replyText);
        } catch (RuntimeException e) {
            idempotency.forget(message.messageId());
            throw e;
        }
    }
}
