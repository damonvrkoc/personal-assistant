package com.personalassistant.memory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personalassistant.config.AssistantProperties;
import com.personalassistant.config.MemoryProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {

    private final Cache<String, List<ConversationTurn>> cache;
    private final int maxMessagesPerUser;

    public ConversationMemoryService(MemoryProperties memoryProperties) {
        var conv = memoryProperties.conversation();
        this.maxMessagesPerUser = conv.maxMessagesPerUser();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(conv.ttl())
                .maximumSize(50_000)
                .build();
    }

    public void appendUser(String waId, String text) {
        append(waId, new ConversationTurn("user", text));
    }

    public void appendAssistant(String waId, String text) {
        append(waId, new ConversationTurn("assistant", text));
    }

    private void append(String waId, ConversationTurn turn) {
        cache.asMap().compute(waId, (k, existing) -> {
            List<ConversationTurn> list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            list.add(turn);
            while (list.size() > maxMessagesPerUser) {
                list.remove(0);
            }
            return list;
        });
    }

    /**
     * Builds messages for the model: system prompt plus recent history (already includes latest user turn
     * if {@link #appendUser} was called first).
     */
    public List<Message> buildModelMessages(String waId, AssistantProperties assistantProperties) {
        List<ConversationTurn> turns = cache.getIfPresent(waId);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(assistantProperties.systemPrompt()));
        if (turns == null || turns.isEmpty()) {
            return messages;
        }
        int max = assistantProperties.maxHistoryMessages();
        int from = Math.max(0, turns.size() - max);
        for (int i = from; i < turns.size(); i++) {
            ConversationTurn t = turns.get(i);
            messages.add(toAiMessage(t));
        }
        return messages;
    }

    private static Message toAiMessage(ConversationTurn t) {
        return switch (t.role()) {
            case "assistant" -> new AssistantMessage(t.content());
            case "user" -> new UserMessage(t.content());
            default -> new UserMessage(t.content());
        };
    }
}
