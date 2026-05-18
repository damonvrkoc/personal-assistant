package com.personalassistant.slack;

import org.springframework.stereotype.Component;

@Component
public class SlackConnectionState {

    public enum Status {
        DISABLED,
        NOT_CONFIGURED,
        CONNECTED,
        FAILED
    }

    private volatile Status status = Status.NOT_CONFIGURED;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
