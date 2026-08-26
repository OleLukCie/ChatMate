package com.chatmate.api.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from synthesis service.
 */
public record SynthesizeResponse(
        @JsonProperty("audio_url") String audioUrl,
        String text,
        Double duration
) {}