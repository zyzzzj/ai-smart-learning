package com.ailearn.config;

import com.ailearn.ai.LlmClient;
import com.ailearn.ai.MockLlmClient;
import com.ailearn.ai.OllamaLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 适配层装配:按 <code>ai.provider</code> 选择 mock 或 ollama 实现。
 */
@Configuration
@Slf4j
public class AiConfig {

    @Bean
    public LlmClient llmClient(AiProperties properties, ObjectMapper objectMapper) {
        if (properties.useOllama()) {
            log.info("AI 提供方:ollama({}),模型:{}",
                    properties.getOllama().getBaseUrl(), properties.getOllama().getModel());
            return new OllamaLlmClient(properties, objectMapper);
        }
        log.info("AI 提供方:mock(内置模拟生成器)。如需真实大模型,设置 AI_PROVIDER=ollama 并启动 Ollama");
        return new MockLlmClient(objectMapper);
    }
}
