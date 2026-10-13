package com.ailearn.controller;

import com.ailearn.ai.LlmClient;
import com.ailearn.common.BusinessException;
import com.ailearn.common.Constants;
import com.ailearn.common.PageResult;
import com.ailearn.common.Result;
import com.ailearn.common.UserContext;
import com.ailearn.dto.AiGenerateQuestionDTO;
import com.ailearn.dto.AiGradeDTO;
import com.ailearn.entity.AiTask;
import com.ailearn.mapper.AiTaskMapper;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.AiAdviceService;
import com.ailearn.service.AiGradingService;
import com.ailearn.service.AiQuestionService;
import com.ailearn.vo.AiAdviceVO;
import com.ailearn.vo.AiGradeResultVO;
import com.ailearn.vo.AiTaskVO;
import com.ailearn.vo.GeneratedQuestionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 能力接口:智能出题、主观题评分、学习建议、任务记录。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 能力", description = "智能出题 / 主观题评分 / 学习建议")
public class AiController {

    private final AiQuestionService aiQuestionService;
    private final AiGradingService aiGradingService;
    private final AiAdviceService aiAdviceService;
    private final LlmClient llmClient;
    private final AiTaskMapper aiTaskMapper;

    @GetMapping("/status")
    @Operation(summary = "当前 AI 提供方与模型")
    public Result<Map<String, Object>> status() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", llmClient.provider());
        map.put("model", llmClient.model());
        map.put("mode", "ollama".equals(llmClient.provider()) ? "真实大模型" : "内置模拟生成器");
        return Result.success(map);
    }

    @PostMapping("/questions/generate")
    @AdminOnly
    @Operation(summary = "AI 出题并入库(管理员)")
    public Result<List<GeneratedQuestionVO>> generate(@Valid @RequestBody AiGenerateQuestionDTO dto) {
        boolean save = dto.getAutoSave() == null || dto.getAutoSave();
        return Result.success(aiQuestionService.generate(dto, save, UserContext.getUserId()));
    }

    @PostMapping("/questions/preview")
    @AdminOnly
    @Operation(summary = "AI 出题预览(不入库)")
    public Result<List<GeneratedQuestionVO>> preview(@Valid @RequestBody AiGenerateQuestionDTO dto) {
        return Result.success(aiQuestionService.generate(dto, false, UserContext.getUserId()));
    }

    @PostMapping("/grade")
    @Operation(summary = "AI 主观题评分")
    public Result<AiGradeResultVO> grade(@Valid @RequestBody AiGradeDTO dto) {
        return Result.success(aiGradingService.grade(dto.getQuestionId(), dto.getStudentAnswer(), UserContext.getUserId()));
    }

    @PostMapping("/advice")
    @Operation(summary = "生成我的学习建议")
    public Result<AiAdviceVO> advice(@RequestParam(required = false) Long subjectId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return Result.success(aiAdviceService.generate(userId, subjectId));
    }

    @GetMapping("/advice/latest")
    @Operation(summary = "我最近一次学习建议")
    public Result<AiAdviceVO> latestAdvice() {
        return Result.success(aiAdviceService.latest(UserContext.getUserId()));
    }

    @GetMapping("/tasks")
    @AdminOnly
    @Operation(summary = "AI 任务记录(管理员查看提示词与返回)")
    public Result<PageResult<AiTaskVO>> tasks(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String type) {
        Page<AiTask> result = aiTaskMapper.selectPage(Page.of(Math.max(1, page), Math.min(size, 100)),
                Wrappers.<AiTask>lambdaQuery()
                        .eq(type != null && !type.isBlank(), AiTask::getType, type)
                        .orderByDesc(AiTask::getId));
        List<AiTaskVO> records = new ArrayList<>();
        for (AiTask task : result.getRecords()) {
            AiTaskVO vo = new AiTaskVO();
            BeanUtils.copyProperties(task, vo);
            records.add(vo);
        }
        return Result.success(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records));
    }

    @GetMapping("/task/{id}")
    @AdminOnly
    @Operation(summary = "AI 任务详情(含提示词与模型原始返回)")
    public Result<AiTask> task(@org.springframework.web.bind.annotation.PathVariable Long id) {
        AiTask task = aiTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!Constants.AI_STATUS_SUCCESS.equals(task.getStatus())) {
            task.setResult(task.getResult());
        }
        return Result.success(task);
    }
}
