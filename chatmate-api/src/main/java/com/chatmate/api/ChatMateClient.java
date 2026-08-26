package com.chatmate.api;

import com.chatmate.api.config.ApiConfig;
import com.chatmate.api.service.InferenceService;
import com.chatmate.api.service.LlmService;

public class ChatMateClient {
    
    private final ApiConfig config;
    private final InferenceService inferenceService;
    private final LlmService llmService;
    
    public ChatMateClient(ApiConfig config) {
        this.config = config;
        this.inferenceService = new InferenceService(config);
        this.llmService = new LlmService(config);
    }
    
    /**
     * Access inference service (transcribe + synthesize).
     */
    public InferenceService inference() {
        return inferenceService;
    }
    
    /**
     * Access LLM chat service.
     */
    public LlmService llm() {
        return llmService;
    }
    
    /**
     * Get current configuration.
     */
    public ApiConfig config() {
        return config;
    }
}
