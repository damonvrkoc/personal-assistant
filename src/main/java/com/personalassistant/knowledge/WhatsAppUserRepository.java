package com.personalassistant.knowledge;

import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface WhatsAppUserRepository extends Neo4jRepository<WhatsAppUserEntity, Long> {

    Optional<WhatsAppUserEntity> findByWaId(String waId);
}
