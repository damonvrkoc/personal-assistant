package com.personalassistant.status;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentStatusService agentStatusService;

    @Test
    void returnsStatusJson() throws Exception {
        when(agentStatusService.getStatus())
                .thenReturn(new AgentStatusResponse(
                        new AgentStatusResponse.ApplicationInfo("personal-assistant", "0.1.0-SNAPSHOT", "PT1M"),
                        new AgentStatusResponse.ChannelsInfo(
                                new AgentStatusResponse.ChannelStatus(true, true, "CONNECTED"),
                                new AgentStatusResponse.ChannelStatus(false, false, "N/A")),
                        new AgentStatusResponse.LlmInfo("openai-compatible", "gpt-4o-mini", "https://api.openai.com", true),
                        new AgentStatusResponse.Neo4jInfo("UP"),
                        new AgentStatusResponse.MemoryInfo(0)));

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.name").value("personal-assistant"))
                .andExpect(jsonPath("$.channels.slack.socketMode").value("CONNECTED"))
                .andExpect(jsonPath("$.neo4j.status").value("UP"));
    }
}
