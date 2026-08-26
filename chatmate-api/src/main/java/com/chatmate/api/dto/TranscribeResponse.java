package com.chatmate.api.dto;

/**
 * Response from transcription service.
 */
public record TranscribeResponse(
    String text,
    String language,
    Double duration
) {}
