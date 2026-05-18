package com.personalassistant.knowledge;

import com.personalassistant.channel.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TurnPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TurnPersistenceService.class);

    private final Neo4jTurnWriter neo4jTurnWriter;
    private final Neo4jPersistenceState neo4jPersistenceState;

    public TurnPersistenceService(Neo4jTurnWriter neo4jTurnWriter, Neo4jPersistenceState neo4jPersistenceState) {
        this.neo4jTurnWriter = neo4jTurnWriter;
        this.neo4jPersistenceState = neo4jPersistenceState;
    }

    @Async
    public void recordTurnAsync(ChannelType channel, String externalId, String role, String text) {
        try {
            neo4jTurnWriter.saveTurn(channel, externalId, role, text);
            neo4jPersistenceState.recordSuccess();
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            neo4jPersistenceState.recordFailure(detail);
            log.warn("Failed to persist turn to Neo4j channel={} user={} role={}", channel, externalId, role, e);
        }
    }
}
