package com.personalassistant.knowledge;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class Neo4jPersistenceState {

    public enum LastWriteStatus {
        NONE,
        OK,
        FAILED
    }

    private volatile LastWriteStatus lastWriteStatus = LastWriteStatus.NONE;
    private volatile String lastFailureDetail;
    private volatile Instant lastFailureAt;

    public LastWriteStatus getLastWriteStatus() {
        return lastWriteStatus;
    }

    public Optional<String> getLastFailureDetail() {
        return lastFailureDetail == null || lastFailureDetail.isBlank()
                ? Optional.empty()
                : Optional.of(lastFailureDetail);
    }

    public Optional<Instant> getLastFailureAt() {
        return Optional.ofNullable(lastFailureAt);
    }

    public void recordSuccess() {
        lastWriteStatus = LastWriteStatus.OK;
        lastFailureDetail = null;
        lastFailureAt = null;
    }

    public void recordFailure(String detail) {
        lastWriteStatus = LastWriteStatus.FAILED;
        lastFailureDetail = detail;
        lastFailureAt = Instant.now();
    }
}
