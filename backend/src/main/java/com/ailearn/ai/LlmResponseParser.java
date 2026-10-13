package com.ailearn.ai;

import com.ailearn.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 大模型响应解析:容错处理代码块包裹、前后废话、字段缺失等情况。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * 从模型返回文本中提取 JSON 对象。
     */
    public JsonNode parseObject(String raw) {
        String json = extractJson(raw, '{', '}');
        if (json == null) {
            throw new BusinessException("大模型未返回可解析的 JSON 内容");
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.warn("JSON 解析失败,原始内容:{}", truncate(raw));
            throw new BusinessException("大模型返回内容不是合法 JSON");
        }
    }

    /**
     * 提取数组(模型有时直接返回数组)。
     */
    public JsonNode parseArrayOrObjectField(String raw, String fieldName) {
        JsonNode node = parseObject(raw);
        if (node.isArray()) {
            return node;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || !field.isArray()) {
            throw new BusinessException("大模型返回结果缺少字段:" + fieldName);
        }
        return field;
    }

    /**
     * 去掉 Markdown 代码块与前后多余文本,截取第一个完整的 JSON 片段。
     */
    private String extractJson(String raw, char open, char close) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        // 去掉 ```json ... ``` 包裹
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd > 0) {
                text = text.substring(firstLineEnd + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        int start = text.indexOf(open);
        if (start < 0) {
            return null;
        }
        // 逐字符扫描,按括号配对找到结束位置(忽略字符串内的括号)
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
