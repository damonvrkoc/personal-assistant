package com.personalassistant.knowledge;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("ChannelUser")
public class ChannelUserEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String channel;

    private String externalId;

    @Relationship(type = "HAD_TURN", direction = Relationship.Direction.OUTGOING)
    private List<ConversationTurnEntity> turns = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public List<ConversationTurnEntity> getTurns() {
        return turns;
    }

    public void setTurns(List<ConversationTurnEntity> turns) {
        this.turns = turns;
    }
}
