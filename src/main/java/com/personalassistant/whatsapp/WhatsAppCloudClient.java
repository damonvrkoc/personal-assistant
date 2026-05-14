package com.personalassistant.whatsapp;

import com.personalassistant.config.WhatsAppCloudProperties;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WhatsAppCloudClient {

    private final WhatsAppCloudProperties props;
    private final RestClient restClient;

    public WhatsAppCloudClient(WhatsAppCloudProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.graphBaseUrl()).build();
    }

    public void sendTextMessage(String toWaId, String text) {
        String path = "/{version}/{phoneNumberId}/messages";
        Map<String, Object> payload =
                Map.of(
                        "messaging_product", "whatsapp",
                        "recipient_type", "individual",
                        "to", toWaId,
                        "type", "text",
                        "text", Map.of("preview_url", false, "body", text));

        restClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .build(props.apiVersion(), props.phoneNumberId()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.accessToken())
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
