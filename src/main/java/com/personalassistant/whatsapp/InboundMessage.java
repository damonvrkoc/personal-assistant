package com.personalassistant.whatsapp;

public record InboundMessage(String waId, String text, String messageId, long timestampSeconds) {}
