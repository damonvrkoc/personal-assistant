package com.personalassistant.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    private final AgentStatusService agentStatusService;

    public StatusController(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    @GetMapping("/api/status")
    public AgentStatusResponse status() {
        return agentStatusService.getStatus();
    }
}
