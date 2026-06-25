package com.cloudbrain.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;

abstract class AbstractOpenAICompatibleProvider implements AIProvider {

    private final String providerName;
    private final String defaultApiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    protected AbstractOpenAICompatibleProvider(String providerName, String defaultApiUrl, ObjectMapper objectMapper) {
        this.providerName = providerName;
        this.defaultApiUrl = defaultApiUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public AIModels.AIChatResponse chat(AIModels.AIChatRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(buildRequest(request, false, null), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parseResponse(request, response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AIProviderException(providerName + " chat interrupted", exception);
        } catch (IOException exception) {
            throw new AIProviderException(providerName + " chat failed", exception);
        }
    }

    @Override
    public AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer) {
        try {
            HttpResponse<InputStream> response = httpClient.send(buildRequest(request, true, chunkConsumer), HttpResponse.BodyHandlers.ofInputStream());
            return parseStreamResponse(request, response, chunkConsumer);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AIProviderException(providerName + " stream interrupted", exception);
        } catch (IOException exception) {
            throw new AIProviderException(providerName + " stream failed", exception);
        }
    }

    private HttpRequest buildRequest(AIModels.AIChatRequest request, boolean stream, Consumer<String> chunkConsumer) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", request.modelName());
        payload.put("stream", stream || request.stream());
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            payload.put("max_tokens", request.maxTokens());
        }
        if (payload.get("stream").asBoolean()) {
            ObjectNode streamOptions = payload.putObject("stream_options");
            streamOptions.put("include_usage", request.includeUsage());
        }
        ArrayNode messages = payload.putArray("messages");
        for (AIModels.AIMessage message : request.messages()) {
            ObjectNode messageNode = messages.addObject();
            messageNode.put("role", Objects.requireNonNullElse(message.role(), "user"));
            List<AIModels.AIContentPart> parts = message.content();
            if (parts == null || parts.isEmpty()) {
                messageNode.put("content", "");
                continue;
            }
            if (parts.size() == 1 && "text".equalsIgnoreCase(parts.get(0).type())) {
                messageNode.put("content", AIModels.safe(parts.get(0).text()));
                continue;
            }
            ArrayNode content = messageNode.putArray("content");
            for (AIModels.AIContentPart part : parts) {
                content.add(toContentNode(part));
            }
        }
        String endpoint = resolveEndpoint(request.apiUrl());
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(10, request.timeoutSeconds())))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + AIModels.firstNonBlank(request.apiKey()))
                .header(HttpHeaders.ACCEPT, payload.get("stream").asBoolean() ? "text/event-stream" : "application/json")
                .header("X-Request-Id", AIModels.firstNonBlank(request.requestId()))
                .header("X-Trace-Id", AIModels.firstNonBlank(request.traceId()));
        if (!payload.get("stream").asBoolean()) {
            builder.header("Accept", "application/json");
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8)).build();
    }

    private ObjectNode toContentNode(AIModels.AIContentPart part) {
        ObjectNode node = objectMapper.createObjectNode();
        String type = AIModels.firstNonBlank(part.type(), "text").toLowerCase(Locale.ROOT);
        switch (type) {
            case "text" -> {
                node.put("type", "text");
                node.put("text", AIModels.safe(part.text()));
            }
            case "image_url" -> {
                node.put("type", "image_url");
                ObjectNode imageUrl = node.putObject("image_url");
                imageUrl.put("url", AIModels.safe(part.url()));
                imageUrl.put("detail", AIModels.firstNonBlank(part.detail(), "auto"));
            }
            case "input_audio" -> {
                node.put("type", "input_audio");
                ObjectNode audio = node.putObject("input_audio");
                audio.put("data", AIModels.safe(part.data()));
                audio.put("format", AIModels.firstNonBlank(part.mimeType(), "mp3"));
            }
            case "video_url" -> {
                node.put("type", "text");
                node.put("text", "[video] " + AIModels.safe(part.url()));
            }
            default -> {
                node.put("type", "text");
                node.put("text", AIModels.safe(part.text()));
            }
        }
        return node;
    }

    private AIModels.AIChatResponse parseResponse(AIModels.AIChatRequest request, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AIProviderException(providerName + " response " + response.statusCode() + ": " + response.body());
        }
        String body = response.body();
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = extractText(root);
            String responseId = root.path("id").asText(null);
            String finishReason = extractFinishReason(root);
            return new AIModels.AIChatResponse(providerName, request.modelName(), request.requestId(), responseId, finishReason, text, body);
        } catch (Exception exception) {
            throw new AIProviderException(providerName + " response parse failed", exception);
        }
    }

    private AIModels.AIChatResponse parseStreamResponse(AIModels.AIChatRequest request,
                                                        HttpResponse<InputStream> response,
                                                        Consumer<String> chunkConsumer) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new AIProviderException(providerName + " stream response " + response.statusCode() + ": " + errorBody);
        }
        StringJoiner raw = new StringJoiner("\n");
        StringBuilder builder = new StringBuilder();
        String responseId = null;
        String finishReason = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                raw.add(line);
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring("data:".length()).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                if (responseId == null) {
                    responseId = root.path("id").asText(null);
                }
                finishReason = extractFinishReason(root);
                String deltaText = extractDeltaText(root);
                if (deltaText != null && !deltaText.isBlank()) {
                    builder.append(deltaText);
                    if (chunkConsumer != null) {
                        chunkConsumer.accept(deltaText);
                    }
                }
            }
        }
        return new AIModels.AIChatResponse(providerName, request.modelName(), request.requestId(), responseId, finishReason, builder.toString(), raw.toString());
    }

    private String resolveEndpoint(String apiUrl) {
        String base = AIModels.firstNonBlank(apiUrl, defaultApiUrl);
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/")) {
            return base + "chat/completions";
        }
        return base + "/chat/completions";
    }

    private String extractText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.path("message");
            String content = extractContent(message.path("content"));
            if (!content.isBlank()) {
                return content;
            }
            String reasoning = extractContent(message.path("reasoning_content"));
            if (!reasoning.isBlank()) {
                return reasoning;
            }
        }
        return "";
    }

    private String extractDeltaText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            JsonNode delta = choice.path("delta");
            String content = extractContent(delta.path("content"));
            if (!content.isBlank()) {
                return content;
            }
            String reasoning = extractContent(delta.path("reasoning_content"));
            if (!reasoning.isBlank()) {
                return reasoning;
            }
        }
        return "";
    }

    private String extractContent(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : node) {
                String text = item.path("text").asText("");
                if (text.isBlank()) {
                    text = item.path("content").asText("");
                }
                if (!text.isBlank()) {
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        return node.asText("");
    }

    private String extractFinishReason(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            String finishReason = choice.path("finish_reason").asText("");
            if (!finishReason.isBlank()) {
                return finishReason;
            }
        }
        return "";
    }
}
