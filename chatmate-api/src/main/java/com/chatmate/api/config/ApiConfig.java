package com.chatmate.api.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for ChatMate API endpoints.
 * Immutable, constructed via builder or loaded from external config.
 */
public final class ApiConfig {
    
    private final String inferenceBaseUrl;
    private final String llmBaseUrl;
    private final String llmApiKey;
    private final Duration timeout;
    private final String model;
    
    private ApiConfig(Builder builder) {
        this.inferenceBaseUrl = Objects.requireNonNull(builder.inferenceBaseUrl, "inferenceBaseUrl required");
        this.llmBaseUrl = Objects.requireNonNull(builder.llmBaseUrl, "llmBaseUrl required");
        this.llmApiKey = builder.llmApiKey;
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ofSeconds(120);
        this.model = builder.model != null ? builder.model : "gpt-3.5-turbo";
    }
    
    public String inferenceBaseUrl() {
        return inferenceBaseUrl;
    }
    
    public String llmBaseUrl() {
        return llmBaseUrl;
    }
    
    public String llmApiKey() {
        return llmApiKey;
    }
    
    public Duration timeout() {
        return timeout;
    }
    
    public String model() {
        return model;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String inferenceBaseUrl;
        private String llmBaseUrl;
        private String llmApiKey;
        private Duration timeout;
        private String model;
        
        public Builder inferenceBaseUrl(String url) {
            this.inferenceBaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            return this;
        }
        
        public Builder llmBaseUrl(String url) {
            this.llmBaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            return this;
        }
        
        public Builder llmApiKey(String key) {
            this.llmApiKey = key;
            return this;
        }
        
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public ApiConfig build() {
            return new ApiConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return "ApiConfig{inference=\"" + inferenceBaseUrl + "\", llm=\"" + llmBaseUrl + "\", model=\"" + model + "\"}";
    }
}
