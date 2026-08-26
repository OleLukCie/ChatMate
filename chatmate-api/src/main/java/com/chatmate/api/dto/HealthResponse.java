package com.chatmate.api.dto;

/**
 * Health check response from inference service.
 */
public record HealthResponse(
    String status,
    boolean whisperLoaded,
    boolean f5ttsLoaded,
    String version
) {}
