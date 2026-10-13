package com.ailearn.service.impl;

import com.ailearn.ai.AiTaskRecorder;
import com.ailearn.ai.LlmClient;
import com.ailearn.ai.LlmResponseParser;
import com.ailearn.ai.PromptTemplateService;
import com.ailearn.common.BusinessException;
import com.ailearn.common.Constants;
import com.ailearn.entity.ExamRecord;
import com.ailearn.entity.LearningAdvice;
import com.ailearn.entity.Subject;
import com.ailearn.mapper.ExamRecordMapper;
import com.ailearn.mapper.LearningAdviceMapper;
import com.ailearn.mapper.PracticeRecordMapper;
import com.ailearn.mapper.SubjectMapper;
import com.ailearn.mapper.WrongQuestionMapper;
import com.ailearn.service.AiAdviceService;
import com.ailearn.vo.AiAdviceVO;
import com.ailearn.vo.KnowledgeStatVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 学习建议实现:把学生的练习数据整理为提示词输入,由模型给出诊断与行动建议。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdviceServiceImpl implements AiAdviceService {

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final LlmResponseParser responseParser;
    private final AiTaskRecorder taskRecorder;
    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final SubjectMapper subjectMapper;
    private final LearningAdviceMapper learningAdviceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAdviceVO generate(Long userId, Long subjectId) {
        // 1. 汇总学生数据
        List<KnowledgeStatVO> stats = practiceRecordMapper.statsByKnowledge(userId);
        Long wrongCount = wrongQuestionMapper.selectCount(Wrappers.<com.ailearn.entity.WrongQuestion>lambdaQuery()
                .eq(com.ailearn.entity.WrongQuestion::getUserId, userId)
                .eq(com.ailearn.entity.WrongQuestion::getMastered, 0));
        Long practiceCount = practiceRecordMapper.selectCount(Wrappers.<com.ailearn.entity.PracticeRecord>lambdaQuery()
                .eq(com.ailearn.entity.PracticeRecord::getUserId, userId));

        List<ExamRecord> records = examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getUserId, userId)
                .in(ExamRecord::getStatus, List.of(Constants.RECORD_SUBMITTED, Constants.RECORD_GRADED)));
        double examAvg = 0;
        if (!records.isEmpty()) {
            double sum = 0;
            for (ExamRecord record : records) {
                sum += record.getTotalScore() == null ? 0 : record.getTotalScore().doubleValue();
            }
            examAvg = Math.round(sum / records.size() * 10) / 10.0;
        }

        String subjectName = "全部科目";
        if (subjectId != null) {
            Subject subject = subjectMapper.selectById(subjectId);
            if (subject != null) {
                subjectName = subject.getName();
            }
        }

        String statsText = stats.isEmpty() ? "暂无练习记录"
                : stats.stream()
                .map(s -> s.getKnowledgePointName() + ":" + s.getRate() + "%")
                .collect(Collectors.joining(","));

        Map<String, Object> vars = new HashMap<>();
        vars.put("subject", subjectName);
        vars.put("knowledgeStats", statsText);
        vars.put("wrongCount", wrongCount == null ? 0 : wrongCount);
        vars.put("practiceCount", practiceCount == null ? 0 : practiceCount);
        vars.put("examAvgScore", examAvg);
        String userPrompt = promptTemplateService.learningAdvicePrompt(vars);
        String systemPrompt = "你是耐心的学习规划师,只输出符合要求的 JSON。";

        Long taskId = taskRecorder.start(Constants.AI_LEARNING_ADVICE, llmClient.provider(), llmClient.model(),
                Map.of("userId", userId, "subjectId", subjectId == null ? 0 : subjectId),
                subjectId, null, userId, userPrompt);

        long start = System.currentTimeMillis();
        String raw;
        try {
            raw = llmClient.chat(systemPrompt, userPrompt);
        } catch (Exception ex) {
            taskRecorder.fail(taskId, null, ex.getMessage(), System.currentTimeMillis() - start);
            throw new BusinessException("AI 生成学习建议失败:" + ex.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        try {
            JsonNode node = responseParser.parseObject(raw);
            String content = node.path("content").asText("");
            if (content.isBlank()) {
                throw new BusinessException("大模型未返回建议内容");
            }
            List<String> weakPoints = new ArrayList<>();
            JsonNode weakNode = node.get("weakPoints");
            if (weakNode != null && weakNode.isArray()) {
                for (JsonNode item : weakNode) {
                    weakPoints.add(item.asText());
                }
            }

            LearningAdvice advice = new LearningAdvice();
            advice.setUserId(userId);
            advice.setSubjectId(subjectId);
            advice.setContent(content);
            advice.setSource("AI");
            advice.setAiTaskId(taskId);
            learningAdviceMapper.insert(advice);

            AiAdviceVO vo = new AiAdviceVO();
            vo.setId(advice.getId());
            vo.setTaskId(taskId);
            vo.setProvider(llmClient.provider());
            vo.setModel(llmClient.model());
            vo.setWeakPoints(weakPoints);
            vo.setContent(content);
            vo.setCreateTime(advice.getCreateTime());
            taskRecorder.success(taskId, raw, vo, cost);
            log.info("AI 学习建议生成完成:userId={}, 薄弱点={}, 耗时 {}ms", userId, weakPoints, cost);
            return vo;
        } catch (Exception ex) {
            taskRecorder.fail(taskId, raw, ex.getMessage(), cost);
            if (ex instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException("学习建议解析失败:" + ex.getMessage());
        }
    }

    @Override
    public AiAdviceVO latest(Long userId) {
        LearningAdvice advice = learningAdviceMapper.selectOne(Wrappers.<LearningAdvice>lambdaQuery()
                .eq(LearningAdvice::getUserId, userId)
                .orderByDesc(LearningAdvice::getId)
                .last("limit 1"));
        if (advice == null) {
            return null;
        }
        AiAdviceVO vo = new AiAdviceVO();
        vo.setId(advice.getId());
        vo.setTaskId(advice.getAiTaskId());
        vo.setContent(advice.getContent());
        vo.setCreateTime(advice.getCreateTime());
        return vo;
    }
}
