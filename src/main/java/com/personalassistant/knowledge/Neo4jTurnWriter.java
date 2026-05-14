package com.personalassistant.knowledge;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Neo4jTurnWriter {

    private final WhatsAppUserRepository userRepository;

    public Neo4jTurnWriter(WhatsAppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveTurn(String waId, String role, String text) {
        WhatsAppUserEntity user = userRepository
                .findByWaId(waId)
                .orElseGet(() -> {
                    WhatsAppUserEntity u = new WhatsAppUserEntity();
                    u.setWaId(waId);
                    return userRepository.save(u);
                });

        ConversationTurnEntity turn = new ConversationTurnEntity();
        turn.setRole(role);
        turn.setText(text);
        turn.setCreatedAt(Instant.now());
        user.getTurns().add(turn);
        userRepository.save(user);
    }
}
