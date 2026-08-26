package com.chatmate.api.dto;

import java.util.List;

/**
 * Request to LLM chat completion API.
 */
public record ChatRequest(
    String model,
    List<ChatMessage> messages,
    Double temperature,
    Integer maxTokens
) {
    public ChatRequest {
        temperature = temperature != null ? temperature : 0.7;
        maxTokens = maxTokens != null ? maxTokens : 400;
    }
}
