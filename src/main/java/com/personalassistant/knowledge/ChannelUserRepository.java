package com.personalassistant.knowledge;

import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ChannelUserRepository extends Neo4jRepository<ChannelUserEntity, Long> {

    Optional<ChannelUserEntity> findByChannelAndExternalId(String channel, String externalId);
}
