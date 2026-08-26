package com.chatmate.api.service;

import com.chatmate.api.config.ApiConfig;
import com.chatmate.api.dto.ChatMessage;
import com.chatmate.api.dto.ChatRequest;
import com.chatmate.api.dto.ChatResponse;
import com.chatmate.api.exception.ChatMateApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for LLM chat completion.
 * Supports both local (ChatMate inference service) and remote (OpenAI‑compatible) APIs.
 */
public class LlmService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfig config;
    private final boolean localMode;

    public LlmService(ApiConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(config.timeout())
                .build();
        // Local mode: LLM baseUrl points to inference service (same host:port)
        this.localMode = config.llmApiKey() == null || config.llmApiKey().isBlank();
    }

    /**
     * Send chat completion request.
     *
     * @param messages Conversation history including system prompt
     * @return Assistant's reply
     */
    public CompletableFuture<ChatResponse> chat(List<ChatMessage> messages) {
        ChatRequest requestBody = new ChatRequest(config.model(), messages, null, null);

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            String endpoint = localMode
                    ? config.llmBaseUrl() + "/api/v1/chat/completions"
                    : config.llmBaseUrl() + "/chat/completions";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(config.timeout());

            if (!localMode && config.llmApiKey() != null && !config.llmApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + config.llmApiKey());
            }

            HttpRequest request = requestBuilder.build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new ChatMateApiException("LLM request failed: " + response.body(), response.statusCode());
                        }
                        try {
                            // Debug output, comment out for production
                            String rawBody = response.body();
                            System.out.println("===LLM RAW RESPONSE JSON===");
                            System.out.println(rawBody);
                            System.out.println("===========================");

                            JsonNode root = objectMapper.readTree(rawBody);
                            String id = root.path("id").asText("");
                            String content = extractContent(root);
                            return new ChatResponse(id, content);
                        } catch (IOException e) {
                            throw new ChatMateApiException("Failed to parse LLM response", e);
                        }
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(new ChatMateApiException("Failed to serialize request", e));
        }
    }

    /**
     * Extract assistant content from multiple LLM response formats.
     * Supports standard OpenAI and llama‑cpp‑python response schema.
     *
     * @param root JsonNode of full http response body
     * @return extracted assistant text content
     * @throws ChatMateApiException when content cannot be extracted
     */
    private String extractContent(JsonNode root) {
        // Try OpenAI standard format: choices[0].message.content
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode first = choices.get(0);
            if (first != null) {
                JsonNode message = first.path("message");
                if (message.isObject()) {
                    String content = message.path("content").asText(null);
                    if (content != null && !content.isBlank()) {
                        return content;
                    }
                }
                // Try alternative field: choices[0].text
                String text = first.path("text").asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }

        // Fallback: top‑level content / text field
        String content = root.path("content").asText(null);
        if (content != null && !content.isBlank()) {
            return content;
        }
        String text = root.path("text").asText(null);
        if (text != null && !text.isBlank()) {
            return text;
        }

        // Use existing two‑arg constructor with status code 500
        throw new ChatMateApiException("Could not extract LLM content. Raw JSON: " + root.toString(), 500);
    }
}
