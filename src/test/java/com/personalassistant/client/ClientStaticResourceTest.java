package com.personalassistant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalassistant.config.ClientWebConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

@SpringBootTest(
        classes = {ClientWebConfig.class, WebMvcAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ClientStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientRootForwardsToIndex() throws Exception {
        mockMvc.perform(get("/client")).andExpect(status().isOk());
    }

    @Test
    void staticAssetsExistOnClasspath() throws Exception {
        String index = StreamUtils.copyToString(
                new ClassPathResource("static/client/index.html").getInputStream(), StandardCharsets.UTF_8);
        String js = StreamUtils.copyToString(
                new ClassPathResource("static/client/app.js").getInputStream(), StandardCharsets.UTF_8);
        assertThat(index).contains("Personal Assistant");
        assertThat(js).contains("fetch");
        assertThat(new ClassPathResource("static/client/styles.css").exists()).isTrue();
    }
}
