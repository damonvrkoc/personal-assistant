package com.personalassistant.knowledge;

import com.personalassistant.channel.ChannelType;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Neo4jTurnWriter {

    private final ChannelUserRepository userRepository;

    public Neo4jTurnWriter(ChannelUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveTurn(ChannelType channel, String externalId, String role, String text) {
        String channelName = channel.name().toLowerCase();
        ChannelUserEntity user = userRepository
                .findByChannelAndExternalId(channelName, externalId)
                .orElseGet(() -> {
                    ChannelUserEntity u = new ChannelUserEntity();
                    u.setChannel(channelName);
                    u.setExternalId(externalId);
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
