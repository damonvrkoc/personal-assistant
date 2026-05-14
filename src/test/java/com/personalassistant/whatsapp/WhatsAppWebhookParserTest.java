package com.personalassistant.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WhatsAppWebhookParserTest {

    private final WhatsAppWebhookParser parser = new WhatsAppWebhookParser(new ObjectMapper());

    @Test
    void parsesTextMessage() {
        String json =
                """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
                      "changes": [
                        {
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "15551234567",
                              "phone_number_id": "PHONE_NUMBER_ID"
                            },
                            "contacts": [
                              {
                                "profile": { "name": "Ada" },
                                "wa_id": "15559876543"
                              }
                            ],
                            "messages": [
                              {
                                "from": "15559876543",
                                "id": "wamid.TEST",
                                "timestamp": "1735689600",
                                "type": "text",
                                "text": { "body": "Hello" }
                              }
                            ]
                          },
                          "field": "messages"
                        }
                      ]
                    }
                  ]
                }
                """;

        var messages = parser.parseTextMessages(json.getBytes());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).waId()).isEqualTo("15559876543");
        assertThat(messages.get(0).text()).isEqualTo("Hello");
        assertThat(messages.get(0).messageId()).isEqualTo("wamid.TEST");
        assertThat(messages.get(0).timestampSeconds()).isEqualTo(1735689600L);
    }
}
