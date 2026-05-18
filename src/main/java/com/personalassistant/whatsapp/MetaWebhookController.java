package com.personalassistant.whatsapp;

import com.personalassistant.assistant.AssistantService;
import com.personalassistant.channel.ChannelMessage;
import com.personalassistant.channel.ChannelType;
import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.WhatsAppCloudProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = ChannelConditions.WHATSAPP_ENABLED, havingValue = "true")
public class MetaWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MetaWebhookController.class);

    private final MetaSignatureVerifier signatureVerifier;
    private final WhatsAppCloudProperties whatsAppCloudProperties;
    private final WhatsAppWebhookParser webhookParser;
    private final AssistantService assistantService;

    public MetaWebhookController(
            MetaSignatureVerifier signatureVerifier,
            WhatsAppCloudProperties whatsAppCloudProperties,
            WhatsAppWebhookParser webhookParser,
            AssistantService assistantService) {
        this.signatureVerifier = signatureVerifier;
        this.whatsAppCloudProperties = whatsAppCloudProperties;
        this.webhookParser = webhookParser;
        this.assistantService = assistantService;
    }

    @GetMapping("/webhook/whatsapp")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode") String mode,
            @RequestParam(name = "hub.verify_token") String token,
            @RequestParam(name = "hub.challenge") String challenge) {
        if (!"subscribe".equals(mode)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        if (!whatsAppCloudProperties.verifyToken().equals(token)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/webhook/whatsapp")
    public ResponseEntity<Void> receive(
            @RequestBody byte[] body,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!signatureVerifier.isValid(body, signature, whatsAppCloudProperties.appSecret())) {
            return ResponseEntity.status(403).build();
        }
        for (InboundMessage message : webhookParser.parseTextMessages(body)) {
            try {
                ChannelMessage channelMessage = new ChannelMessage(
                        ChannelType.WHATSAPP,
                        message.waId(),
                        message.text(),
                        message.messageId(),
                        message.timestampSeconds(),
                        message.waId());
                assistantService.handle(channelMessage);
            } catch (RuntimeException e) {
                log.error("Failed to handle inbound messageId={}", message.messageId(), e);
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.ok().build();
    }
}
