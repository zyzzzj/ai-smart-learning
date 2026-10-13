package com.ailearn.ai;

import com.ailearn.common.Constants;
import com.ailearn.entity.AiTask;
import com.ailearn.mapper.AiTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 任务记录器:统一记录提示词、模型原始返回、解析结果、耗时与错误。
 * 所有 AI 调用都留痕,便于排查与展示"AI 出题/评分过程"。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskRecorder {

    private final AiTaskMapper aiTaskMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建任务并置为 RUNNING。
     */
    public Long start(String type, String provider, String model, Object params,
                      Long subjectId, Long knowledgePointId, Long userId, String prompt) {
        AiTask task = new AiTask();
        task.setType(type);
        task.setProvider(provider);
        task.setModel(model);
        task.setSubjectId(subjectId);
        task.setKnowledgePointId(knowledgePointId);
        task.setUserId(userId);
        task.setRequestParams(toJson(params));
        task.setPrompt(prompt);
        task.setStatus(Constants.AI_STATUS_RUNNING);
        aiTaskMapper.insert(task);
        return task.getId();
    }

    /**
     * 任务成功结束。
     */
    public void success(Long taskId, String response, Object result, long costMs) {
        finish(taskId, Constants.AI_STATUS_SUCCESS, response, toJson(result), null, costMs);
    }

    /**
     * 任务失败结束。
     */
    public void fail(Long taskId, String response, String error, long costMs) {
        finish(taskId, Constants.AI_STATUS_FAILED, response, null, error, costMs);
    }

    private void finish(Long taskId, String status, String response, String result, String error, long costMs) {
        if (taskId == null) {
            return;
        }
        try {
            AiTask update = new AiTask();
            update.setId(taskId);
            update.setStatus(status);
            update.setResponse(truncate(response, 60000));
            update.setResult(result);
            update.setError(truncate(error, 500));
            update.setCostMs(costMs);
            update.setFinishTime(LocalDateTime.now());
            aiTaskMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("AI 任务记录落库失败:{}", ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
