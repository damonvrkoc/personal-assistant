package com.personalassistant.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.personalassistant.config.SlackProperties;
import org.junit.jupiter.api.Test;

class SlackUserResolverIdTest {

    @Test
    void acceptsSlackUserIdWithoutApiCall() {
        var resolver = new SlackUserResolver(new SlackProperties("x", "y", true, "damon.vrkoc"));
        assertThat(resolver.resolveUserId("U012ABCDEF")).contains("U012ABCDEF");
        assertThat(resolver.resolveUserId("@U012ABCDEF")).contains("U012ABCDEF");
    }
}
