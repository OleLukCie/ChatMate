package com.chatmate.api.dto;

/**
 * Request to synthesize speech from text.
 */
public record SynthesizeRequest(
    String text,
    String refAudio,
    String refText,
    Integer nfeStep,
    Double speed
) {
    public SynthesizeRequest {
        speed = speed != null ? speed : 1.0;
    }
    
    public SynthesizeRequest(String text) {
        this(text, null, null, null, 1.0);
    }
}
