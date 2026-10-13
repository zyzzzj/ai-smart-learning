package com.ailearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型配置。
 */
@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {

    /** mock = 内置模拟生成;ollama = 调用本地 Ollama */
    private String provider = "mock";

    private Ollama ollama = new Ollama();
    private Prompt prompt = new Prompt();

    @Data
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "deepseek-r1:7b";
        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 300;
    }

    @Data
    public static class Prompt {
        private String generateQuestionFile = "prompts/generate-question.txt";
        private String gradeSubjectiveFile = "prompts/grade-subjective.txt";
        private String adviceFile = "prompts/learning-advice.txt";
    }

    public boolean useOllama() {
        return "ollama".equalsIgnoreCase(provider);
    }
}
