package com.chatmate.api.dto;

/**
 * Request to transcribe audio to text.
 */
public record TranscribeRequest(
    String language,
    String task
) {
    public TranscribeRequest {
        language = language != null ? language : "en";
        task = task != null ? task : "transcribe";
    }
    
    public TranscribeRequest() {
        this("en", "transcribe");
    }
}
