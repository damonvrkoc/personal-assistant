package com.personalassistant.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import com.personalassistant.config.ChannelConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.WHATSAPP_ENABLED, havingValue = "true")
public class WhatsAppWebhookParser {

    private final ObjectMapper objectMapper;

    public WhatsAppWebhookParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<InboundMessage> parseTextMessages(byte[] rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            List<InboundMessage> out = new ArrayList<>();
            JsonNode entries = root.path("entry");
            if (!entries.isArray()) {
                return out;
            }
            for (JsonNode entry : entries) {
                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) {
                    continue;
                }
                for (JsonNode change : changes) {
                    JsonNode value = change.path("value");
                    JsonNode messages = value.path("messages");
                    if (!messages.isArray()) {
                        continue;
                    }
                    for (JsonNode message : messages) {
                        if (!"text".equals(message.path("type").asText())) {
                            continue;
                        }
                        String from = message.path("from").asText(null);
                        String id = message.path("id").asText(null);
                        String body = message.path("text").path("body").asText(null);
                        long ts = message.path("timestamp").asLong(0L);
                        if (from != null && id != null && body != null) {
                            out.add(new InboundMessage(from, body, id, ts));
                        }
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }
}
