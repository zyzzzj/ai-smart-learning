package com.ailearn.service.impl;

import com.ailearn.ai.AiTaskRecorder;
import com.ailearn.ai.LlmClient;
import com.ailearn.ai.LlmResponseParser;
import com.ailearn.ai.PromptTemplateService;
import com.ailearn.common.BusinessException;
import com.ailearn.common.CacheNames;
import com.ailearn.common.Constants;
import com.ailearn.common.QuestionValidator;
import com.ailearn.dto.AiGenerateQuestionDTO;
import com.ailearn.dto.QuestionOption;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.entity.Question;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.KnowledgePointMapper;
import com.ailearn.mapper.QuestionMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.service.AiQuestionService;
import com.ailearn.vo.GeneratedQuestionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 出题实现:构造提示词 → 调用大模型(或模拟器)→ 解析 → 结构校验 → 入库 → 记录 ai_task。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuestionServiceImpl implements AiQuestionService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            Constants.TYPE_SINGLE, Constants.TYPE_MULTIPLE, Constants.TYPE_JUDGE,
            Constants.TYPE_FILL, Constants.TYPE_SUBJECTIVE);

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final LlmResponseParser responseParser;
    private final AiTaskRecorder taskRecorder;
    private final QuestionValidator questionValidator;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CacheNames.HOT_QUESTIONS, CacheNames.QUESTION_DETAIL}, allEntries = true)
    public List<GeneratedQuestionVO> generate(AiGenerateQuestionDTO dto, boolean save, Long userId) {
        Subject subject = subjectMapper.selectById(dto.getSubjectId());
        if (subject == null) {
            throw new BusinessException("科目不存在");
        }
        String type = dto.getType() == null ? Constants.TYPE_SINGLE : dto.getType().toUpperCase();
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BusinessException("不支持的题型:" + type);
        }
        int count = dto.getCount() == null ? 3 : Math.max(1, Math.min(dto.getCount(), 10));
        int difficulty = dto.getDifficulty() == null ? 3 : Math.max(1, Math.min(dto.getDifficulty(), 5));

        String knowledgeName = subject.getName();
        if (dto.getKnowledgePointId() != null) {
            KnowledgePoint kp = knowledgePointMapper.selectById(dto.getKnowledgePointId());
            if (kp == null) {
                throw new BusinessException("知识点不存在");
            }
            knowledgeName = kp.getName();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("subject", subject.getName());
        vars.put("knowledgePoint", knowledgeName);
        vars.put("type", type);
        vars.put("difficulty", difficulty);
        vars.put("count", count);
        String userPrompt = promptTemplateService.generateQuestionPrompt(vars);
        String systemPrompt = "你是资深的" + subject.getName() + "命题专家,只输出符合要求的 JSON。";

        Long taskId = taskRecorder.start(Constants.AI_GENERATE_QUESTION, llmClient.provider(), llmClient.model(),
                dto, dto.getSubjectId(), dto.getKnowledgePointId(), userId, userPrompt);

        long start = System.currentTimeMillis();
        String raw;
        try {
            raw = llmClient.chat(systemPrompt, userPrompt);
        } catch (Exception ex) {
            taskRecorder.fail(taskId, null, ex.getMessage(), System.currentTimeMillis() - start);
            throw new BusinessException("AI 出题失败:" + ex.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        List<GeneratedQuestionVO> result = new ArrayList<>();
        try {
            JsonNode array = responseParser.parseArrayOrObjectField(raw, "questions");
            for (JsonNode node : array) {
                GeneratedQuestionVO vo = toVO(node, type, difficulty, dto.getScore(), dto.getTags());
                if (Boolean.TRUE.equals(vo.getValid()) && save) {
                    Question question = persist(vo, dto, type, taskId);
                    vo.setQuestionId(question.getId());
                }
                result.add(vo);
            }
            if (result.isEmpty()) {
                throw new BusinessException("大模型未生成任何题目");
            }
            taskRecorder.success(taskId, raw, result, cost);
        } catch (Exception ex) {
            taskRecorder.fail(taskId, raw, ex.getMessage(), cost);
            if (ex instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException("AI 出题结果解析失败:" + ex.getMessage());
        }
        log.info("AI 出题完成:provider={}, 生成 {} 道,入库={}, 耗时 {}ms",
                llmClient.provider(), result.size(), save, cost);
        return result;
    }

    private GeneratedQuestionVO toVO(JsonNode node, String defaultType, int defaultDifficulty,
                                     BigDecimal defaultScore, String defaultTags) {
        GeneratedQuestionVO vo = new GeneratedQuestionVO();
        String type = node.path("type").asText(defaultType).toUpperCase();
        vo.setType(type);
        vo.setStem(node.path("stem").asText(""));
        vo.setAnswer(node.path("answer").asText(""));
        vo.setAnalysis(node.path("analysis").asText(""));
        vo.setDifficulty(node.path("difficulty").asInt(defaultDifficulty));
        vo.setScore(node.hasNonNull("score") ? new BigDecimal(node.path("score").asText("5"))
                : (defaultScore == null ? new BigDecimal("5") : defaultScore));
        String tags = node.path("tags").asText("");
        vo.setTags(blankToDefault(tags, defaultTags));
        vo.setOptions(parseOptions(node.get("options")));

        String reason = questionValidator.validate(vo.getType(), vo.getOptions(), vo.getAnswer());
        if (reason == null && vo.getStem().isBlank()) {
            reason = "题干为空";
        }
        vo.setValid(reason == null);
        vo.setInvalidReason(reason);
        return vo;
    }

    private List<QuestionOption> parseOptions(JsonNode optionsNode) {
        List<QuestionOption> options = new ArrayList<>();
        if (optionsNode == null || !optionsNode.isArray()) {
            return options;
        }
        for (JsonNode node : optionsNode) {
            QuestionOption option = new QuestionOption();
            option.setKey(node.path("key").asText(""));
            option.setContent(node.path("content").asText(""));
            options.add(option);
        }
        return options;
    }

    private Question persist(GeneratedQuestionVO vo, AiGenerateQuestionDTO dto, String type, Long taskId) {
        Question question = new Question();
        question.setSubjectId(dto.getSubjectId());
        question.setKnowledgePointId(dto.getKnowledgePointId());
        question.setType(vo.getType());
        question.setStem(vo.getStem());
        question.setAnswer(vo.getAnswer());
        question.setAnalysis(vo.getAnalysis());
        question.setDifficulty(vo.getDifficulty());
        question.setScore(vo.getScore());
        question.setTags(vo.getTags());
        question.setSource(Constants.SOURCE_AI);
        question.setAiTaskId(taskId);
        question.setStatus(1);
        question.setViewCount(0);
        question.setCorrectCount(0);
        question.setCreateUser(dto.getSubjectId() == null ? null : null);
        try {
            if (!vo.getOptions().isEmpty()) {
                question.setOptions(objectMapper.writeValueAsString(vo.getOptions()));
            }
        } catch (Exception ex) {
            throw new BusinessException("选项序列化失败");
        }
        questionMapper.insert(question);
        return question;
    }

    private String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue == null ? "AI生成" : defaultValue;
        }
        return value;
    }
}
