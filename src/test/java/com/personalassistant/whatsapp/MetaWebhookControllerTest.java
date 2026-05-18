package com.personalassistant.whatsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalassistant.assistant.AssistantService;
import com.personalassistant.config.WhatsAppCloudProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetaWebhookController.class)
@EnableConfigurationProperties(WhatsAppCloudProperties.class)
@Import({MetaSignatureVerifier.class, WhatsAppWebhookParser.class})
@TestPropertySource(
        properties = {
            "app.channels.whatsapp.enabled=true",
            "app.channels.slack.enabled=false",
            "whatsapp.cloud.verify-token=test-verify",
            "whatsapp.cloud.app-secret=test-secret",
            "whatsapp.cloud.access-token=test-access",
            "whatsapp.cloud.phone-number-id=123",
            "whatsapp.cloud.api-version=v21.0",
            "whatsapp.cloud.graph-base-url=https://graph.facebook.com"
        })
class MetaWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @Test
    void verifyReturnsChallengeWhenTokenMatches() throws Exception {
        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify")
                        .param("hub.challenge", "CHALLENGE_ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(content().string("CHALLENGE_ACCEPTED"));
    }

    @Test
    void verifyForbiddenWhenTokenMismatch() throws Exception {
        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong")
                        .param("hub.challenge", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postRejectsInvalidSignature() throws Exception {
        mockMvc.perform(post("/webhook/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=deadbeef")
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(assistantService, never()).handle(any());
    }
}
