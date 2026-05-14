package com.personalassistant.knowledge;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("WhatsAppUser")
public class WhatsAppUserEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String waId;

    @Relationship(type = "HAD_TURN", direction = Relationship.Direction.OUTGOING)
    private List<ConversationTurnEntity> turns = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getWaId() {
        return waId;
    }

    public void setWaId(String waId) {
        this.waId = waId;
    }

    public List<ConversationTurnEntity> getTurns() {
        return turns;
    }

    public void setTurns(List<ConversationTurnEntity> turns) {
        this.turns = turns;
    }
}
