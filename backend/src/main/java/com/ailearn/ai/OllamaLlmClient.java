package com.ailearn.ai;

import com.ailearn.common.BusinessException;
import com.ailearn.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama 实现:调用本地部署的大模型(默认 DeepSeek)。
 *
 * <p>接口:<code>POST {baseUrl}/api/chat</code>,请求体为 Ollama 原生格式,
 * <code>stream=false</code> 且 <code>format=json</code>,便于直接拿到结构化结果。</p>
 */
@Slf4j
public class OllamaLlmClient implements LlmClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaLlmClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        AiProperties.Ollama ollama = properties.getOllama();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, ollama.getConnectTimeoutSeconds())))
                .build();
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public String model() {
        return properties.getOllama().getModel();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        AiProperties.Ollama ollama = properties.getOllama();
        String url = ollama.getBaseUrl().replaceAll("/+$", "") + "/api/chat";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ollama.getModel());
        body.put("messages", messages);
        body.put("stream", false);
        body.put("format", "json");
        body.put("options", Map.of("temperature", 0.7));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new BusinessException("构造模型请求失败:" + ex.getMessage());
        }

        // 简单重试:大模型偶发超时/连接失败时再试一次
        int attempts = 2;
        Exception last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(Math.max(10, ollama.getReadTimeoutSeconds())))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    throw new BusinessException("Ollama 返回状态码 " + response.statusCode() + ":" + truncate(response.body()));
                }
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode content = root.path("message").path("content");
                if (content.isMissingNode() || content.asText().isBlank()) {
                    throw new BusinessException("Ollama 返回内容为空");
                }
                return content.asText();
            } catch (Exception ex) {
                last = ex;
                log.warn("调用 Ollama 失败(第 {} 次):{}", i, ex.getMessage());
            }
        }
        throw new BusinessException("调用本地大模型失败,请确认 Ollama 已启动且模型已拉取("
                + ollama.getModel() + "):" + (last == null ? "未知错误" : last.getMessage()));
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
