package com.personalassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    WhatsAppCloudProperties.class,
    AssistantProperties.class,
    MemoryProperties.class,
    AppSecurityProperties.class
})
public class PropertiesConfig {}
