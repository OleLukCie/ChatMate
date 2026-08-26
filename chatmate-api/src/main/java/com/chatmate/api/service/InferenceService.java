package com.chatmate.api.service;
import com.chatmate.api.config.ApiConfig;
import com.chatmate.api.dto.*;
import com.chatmate.api.exception.ChatMateApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for ChatMate inference service (transcribe + synthesize).
 */
public class InferenceService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfig config;

    public InferenceService(ApiConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        // HTTP1.1
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Check if inference service is healthy.
     */
    public CompletableFuture<HealthResponse> healthCheck() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.inferenceBaseUrl() + "/health"))
                .GET()
                .timeout(config.timeout())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new ChatMateApiException("Health check failed: " + response.body(), response.statusCode());
                    }
                    try {
                        return objectMapper.readValue(response.body(), HealthResponse.class);
                    } catch (IOException e) {
                        throw new ChatMateApiException("Failed to parse health response", e);
                    }
                });
    }

    /**
     * Transcribe audio file to text.
     * Uses multipart/form-data with correct boundary formatting.
     */
    public CompletableFuture<TranscribeResponse> transcribe(Path audioPath, String language, String task) {
        try {
            byte[] audioBytes = Files.readAllBytes(audioPath);
            String filename = audioPath.getFileName().toString();
            String boundary = "----JavaBoundary" + System.currentTimeMillis();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Part 1: audio file
            writePart(baos, boundary, "audio", filename, "audio/wav", audioBytes);

            // Part 2: language
            writePart(baos, boundary, "language", null, null, language.getBytes(StandardCharsets.UTF_8));

            // Part 3: task
            writePart(baos, boundary, "task", null, null, task.getBytes(StandardCharsets.UTF_8));

            // Final boundary
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            byte[] body = baos.toByteArray();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.inferenceBaseUrl() + "/api/v1/transcribe"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(config.timeout())
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new ChatMateApiException("Transcription failed: " + response.body(), response.statusCode());
                        }
                        try {
                            return objectMapper.readValue(response.body(), TranscribeResponse.class);
                        } catch (IOException e) {
                            throw new ChatMateApiException("Failed to parse transcription response", e);
                        }
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(new ChatMateApiException("Failed to read audio file", e));
        }
    }

    private void writePart(ByteArrayOutputStream out, String boundary, String name,
                           String filename, String contentType, byte[] data) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));

        if (filename != null) {
            out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        } else {
            out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        }

        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Synthesize speech from text.
     */
    public CompletableFuture<SynthesizeResponse> synthesize(String text, Integer nfeStep) {
        SynthesizeRequest requestBody = new SynthesizeRequest(text, null, null, nfeStep, null);

        try {
            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.inferenceBaseUrl() + "/api/v1/synthesize"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(config.timeout())
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new ChatMateApiException("Synthesis failed: " + response.body(), response.statusCode());
                        }
                        try {
                            return objectMapper.readValue(response.body(), SynthesizeResponse.class);
                        } catch (IOException e) {
                            throw new ChatMateApiException("Failed to parse synthesis response", e);
                        }
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(new ChatMateApiException("Failed to serialize request", e));
        }
    }

    /**
     * Download synthesized audio file.
     */
    public CompletableFuture<byte[]> downloadAudio(String audioUrl) {
        String fullUrl = audioUrl.startsWith("http") ? audioUrl : config.inferenceBaseUrl() + audioUrl;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .GET()
                .timeout(config.timeout())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new ChatMateApiException("Download failed: " + response.statusCode(), response.statusCode());
                    }
                    return response.body();
                });
    }
}