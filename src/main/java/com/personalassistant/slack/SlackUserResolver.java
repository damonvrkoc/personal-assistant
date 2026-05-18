package com.personalassistant.slack;

import com.personalassistant.config.ChannelConditions;
import com.personalassistant.config.SlackProperties;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ChannelConditions.SLACK_ENABLED, havingValue = "true")
public class SlackUserResolver {

    private static final String SLACK_USER_ID_PATTERN = "^U[A-Z0-9]+$";

    private final SlackProperties properties;
    private final Slack slack = Slack.getInstance();

    public SlackUserResolver(SlackProperties properties) {
        this.properties = properties;
    }

    public Optional<String> resolveUserId(String handleOrUserId) {
        if (handleOrUserId == null || handleOrUserId.isBlank()) {
            return Optional.empty();
        }
        String needle = handleOrUserId.trim();
        if (needle.startsWith("@")) {
            needle = needle.substring(1);
        }
        if (needle.matches(SLACK_USER_ID_PATTERN)) {
            return Optional.of(needle);
        }
        return findUserIdByHandle(needle);
    }

    private Optional<String> findUserIdByHandle(String handle) {
        String normalizedHandle = handle.toLowerCase(Locale.ROOT);
        try {
            String cursor = null;
            do {
                final String pageCursor = cursor;
                UsersListResponse response = slack.methods(properties.botToken())
                        .usersList(req -> req.limit(200).cursor(pageCursor));
                if (!response.isOk()) {
                    throw new IllegalStateException("Slack users.list failed: " + response.getError());
                }
                for (User user : response.getMembers()) {
                    if (Boolean.TRUE.equals(user.isDeleted())) {
                        continue;
                    }
                    if (matchesHandle(user, normalizedHandle)) {
                        return Optional.of(user.getId());
                    }
                }
                cursor = response.getResponseMetadata() != null
                        ? response.getResponseMetadata().getNextCursor()
                        : null;
            } while (cursor != null && !cursor.isBlank());
        } catch (IOException | SlackApiException e) {
            throw new IllegalStateException("Slack users.list failed", e);
        }
        return Optional.empty();
    }

    private static boolean matchesHandle(User user, String normalizedHandle) {
        if (normalizedHandle.equalsIgnoreCase(user.getId())) {
            return true;
        }
        if (user.getName() != null && user.getName().equalsIgnoreCase(normalizedHandle)) {
            return true;
        }
        if (user.getProfile() != null) {
            String displayName = user.getProfile().getDisplayName();
            if (displayName != null && displayName.equalsIgnoreCase(normalizedHandle)) {
                return true;
            }
            String realName = user.getProfile().getRealName();
            if (realName != null && realName.equalsIgnoreCase(normalizedHandle)) {
                return true;
            }
            String realNameNorm = user.getProfile().getRealNameNormalized();
            if (realNameNorm != null && realNameNorm.equalsIgnoreCase(normalizedHandle)) {
                return true;
            }
        }
        return false;
    }
}
