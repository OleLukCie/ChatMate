package com.chatmate.api.dto;

/**
 * Response from LLM chat completion API.
 */
public record ChatResponse(
    String id,
    String content
) {}
