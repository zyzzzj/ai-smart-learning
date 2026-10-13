package com.ailearn.service.impl;

import com.ailearn.ai.AiTaskRecorder;
import com.ailearn.ai.LlmClient;
import com.ailearn.ai.LlmResponseParser;
import com.ailearn.ai.PromptTemplateService;
import com.ailearn.common.BusinessException;
import com.ailearn.common.Constants;
import com.ailearn.entity.Question;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.service.AiGradingService;
import com.ailearn.vo.AiGradeResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 主观题评分实现:要点覆盖度评分 + 评语,结果全部留痕到 ai_task。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGradingServiceImpl implements AiGradingService {

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final LlmResponseParser responseParser;
    private final AiTaskRecorder taskRecorder;
    private final QuestionMapper questionMapper;

    @Override
    public AiGradeResultVO grade(Long questionId, String studentAnswer, Long userId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        if (!Constants.TYPE_SUBJECTIVE.equals(question.getType())) {
            throw new BusinessException("只有主观题需要 AI 评分,客观题由系统自动判分");
        }
        if (question.getAnswer() == null || question.getAnswer().isBlank()) {
            throw new BusinessException("该主观题缺少参考答案,无法评分");
        }
        double fullScore = question.getScore() == null ? 10.0 : question.getScore().doubleValue();

        Map<String, Object> vars = new HashMap<>();
        vars.put("stem", question.getStem());
        vars.put("referenceAnswer", question.getAnswer());
        vars.put("studentAnswer", studentAnswer);
        vars.put("fullScore", trimNumber(fullScore));
        String userPrompt = promptTemplateService.gradeSubjectivePrompt(vars);
        String systemPrompt = "你是严谨的阅卷老师,只输出符合要求的 JSON。";

        Long taskId = taskRecorder.start(Constants.AI_GRADE_SUBJECTIVE, llmClient.provider(), llmClient.model(),
                Map.of("questionId", questionId, "answerLength", studentAnswer.length()),
                question.getSubjectId(), question.getKnowledgePointId(), userId, userPrompt);

        long start = System.currentTimeMillis();
        String raw;
        try {
            raw = llmClient.chat(systemPrompt, userPrompt);
        } catch (Exception ex) {
            taskRecorder.fail(taskId, null, ex.getMessage(), System.currentTimeMillis() - start);
            throw new BusinessException("AI 评分失败:" + ex.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        try {
            JsonNode node = responseParser.parseObject(raw);
            double score = node.path("score").asDouble(0);
            if (score < 0) {
                score = 0;
            }
            if (score > fullScore) {
                score = fullScore;
            }
            AiGradeResultVO vo = new AiGradeResultVO();
            vo.setTaskId(taskId);
            vo.setProvider(llmClient.provider());
            vo.setModel(llmClient.model());
            vo.setQuestionId(questionId);
            vo.setScore(Math.round(score * 10) / 10.0);
            vo.setFullScore(fullScore);
            vo.setHitPoints(readStringList(node.get("hitPoints")));
            vo.setMissingPoints(readStringList(node.get("missingPoints")));
            vo.setFeedback(node.path("feedback").asText(""));
            vo.setConfidence(node.path("confidence").asDouble(0.6));
            vo.setCostMs(cost);
            taskRecorder.success(taskId, raw, vo, cost);
            return vo;
        } catch (Exception ex) {
            taskRecorder.fail(taskId, raw, ex.getMessage(), cost);
            throw new BusinessException("AI 评分结果解析失败:" + ex.getMessage());
        }
    }

    private List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private BigDecimal trimNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros();
    }
}
