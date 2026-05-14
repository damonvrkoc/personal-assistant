package com.personalassistant.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalassistant.config.WhatsAppCloudProperties;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhatsAppCloudClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockWebServer server;
    private WhatsAppCloudClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        var props = new WhatsAppCloudProperties(
                "v",
                "s",
                "token",
                "phone-id",
                "v21.0",
                "http://localhost:" + server.getPort());
        client = new WhatsAppCloudClient(props);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void postsTextMessageToGraphApi() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"messages\":[{\"id\":\"mid\"}]}"));

        client.sendTextMessage("15551234567", "hi");

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getPath()).isEqualTo("/v21.0/phone-id/messages");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer token");

        JsonNode body = objectMapper.readTree(req.getBody().readUtf8());
        assertThat(body.path("to").asText()).isEqualTo("15551234567");
        assertThat(body.path("type").asText()).isEqualTo("text");
        assertThat(body.path("text").path("body").asText()).isEqualTo("hi");
    }
}
