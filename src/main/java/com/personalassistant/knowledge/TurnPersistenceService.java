package com.personalassistant.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TurnPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TurnPersistenceService.class);

    private final Neo4jTurnWriter neo4jTurnWriter;

    public TurnPersistenceService(Neo4jTurnWriter neo4jTurnWriter) {
        this.neo4jTurnWriter = neo4jTurnWriter;
    }

    @Async
    public void recordTurnAsync(String waId, String role, String text) {
        try {
            neo4jTurnWriter.saveTurn(waId, role, text);
        } catch (Exception e) {
            log.warn("Failed to persist turn to Neo4j waId={} role={}", waId, role, e);
        }
    }
}
