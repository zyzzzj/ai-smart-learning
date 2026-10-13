package com.ailearn.ai;

import com.ailearn.common.BusinessException;
import com.ailearn.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板加载与变量填充。
 * 两条链路(mock / ollama)共用同一套模板,保证输出契约一致。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final AiProperties properties;
    private final Map<String, String> templates = new HashMap<>();

    @PostConstruct
    public void load() {
        templates.put("generate", read(properties.getPrompt().getGenerateQuestionFile()));
        templates.put("grade", read(properties.getPrompt().getGradeSubjectiveFile()));
        templates.put("advice", read(properties.getPrompt().getAdviceFile()));
        log.info("提示词模板加载完成:{}", templates.keySet());
    }

    public String generateQuestionPrompt(Map<String, Object> vars) {
        return fill(templates.get("generate"), vars);
    }

    public String gradeSubjectivePrompt(Map<String, Object> vars) {
        return fill(templates.get("grade"), vars);
    }

    public String learningAdvicePrompt(Map<String, Object> vars) {
        return fill(templates.get("advice"), vars);
    }

    private String fill(String template, Map<String, Object> vars) {
        if (template == null) {
            throw new BusinessException("提示词模板未加载");
        }
        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    private String read(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("提示词模板读取失败:{}", path, ex);
            throw new IllegalStateException("提示词模板读取失败:" + path, ex);
        }
    }
}
