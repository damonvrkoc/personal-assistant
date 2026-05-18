package com.personalassistant.status;

public record AgentStatusResponse(
        ApplicationInfo application,
        ChannelsInfo channels,
        LlmInfo llm,
        Neo4jInfo neo4j,
        MemoryInfo memory) {

    public record ApplicationInfo(String name, String version, String uptime) {}

    public record ChannelsInfo(ChannelStatus slack, ChannelStatus whatsapp) {}

    public record ChannelStatus(boolean enabled, boolean configured, String socketMode) {}

    public record LlmInfo(String provider, String model, String baseUrl, boolean apiKeyConfigured) {}

    public record Neo4jInfo(String status) {}

    public record MemoryInfo(int activeConversations) {}
}
