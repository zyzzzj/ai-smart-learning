package com.ailearn.controller;

import com.ailearn.common.BusinessException;
import com.ailearn.common.PageResult;
import com.ailearn.common.Result;
import com.ailearn.common.UserContext;
import com.ailearn.dto.PracticeStartDTO;
import com.ailearn.dto.PracticeSubmitDTO;
import com.ailearn.service.LearningStatsService;
import com.ailearn.service.PracticeService;
import com.ailearn.vo.LearningStatsVO;
import com.ailearn.vo.PracticeSessionVO;
import com.ailearn.vo.PracticeStartVO;
import com.ailearn.vo.PracticeSubmitResultVO;
import com.ailearn.vo.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能刷题接口。
 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@Tag(name = "智能刷题", description = "随机/知识点/难度/错题本刷题 + 学习统计")
public class PracticeController {

    private final PracticeService practiceService;
    private final LearningStatsService learningStatsService;

    @PostMapping("/start")
    @Operation(summary = "开始刷题(模式:RANDOM/KNOWLEDGE/DIFFICULTY/WRONG)")
    public Result<PracticeStartVO> start(@RequestBody PracticeStartDTO dto) {
        return Result.success(practiceService.start(dto, currentUserId()));
    }

    @PostMapping("/{sessionId}/submit")
    @Operation(summary = "提交单题作答并即时判分(主观题走 AI 评分)")
    public Result<PracticeSubmitResultVO> submit(@PathVariable Long sessionId,
                                                 @Valid @RequestBody PracticeSubmitDTO dto) {
        return Result.success(practiceService.submit(sessionId, dto, currentUserId()));
    }

    @PostMapping("/{sessionId}/finish")
    @Operation(summary = "结束本次刷题")
    public Result<Void> finish(@PathVariable Long sessionId) {
        practiceService.finish(sessionId, currentUserId());
        return Result.success();
    }

    @GetMapping("/sessions")
    @Operation(summary = "我的刷题记录")
    public Result<PageResult<PracticeSessionVO>> sessions(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(practiceService.mySessions(page, size, currentUserId()));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "刷题会话详情")
    public Result<PracticeSessionVO> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(practiceService.detail(sessionId, currentUserId()));
    }

    @GetMapping("/wrong-book")
    @Operation(summary = "错题本(mastered=0 未掌握,1 已掌握)")
    public Result<List<WrongQuestionVO>> wrongBook(@RequestParam(required = false) Integer mastered,
                                                   @RequestParam(required = false) Long knowledgePointId,
                                                   @RequestParam(defaultValue = "50") int limit) {
        return Result.success(practiceService.wrongBook(mastered, knowledgePointId, limit, currentUserId()));
    }

    @DeleteMapping("/wrong-book/{questionId}")
    @Operation(summary = "从错题本移除")
    public Result<Void> removeFromWrongBook(@PathVariable Long questionId) {
        practiceService.removeFromWrongBook(questionId, currentUserId());
        return Result.success();
    }

    @GetMapping("/stats")
    @Operation(summary = "我的学习统计(正确率/知识点分布/7 天趋势/薄弱点)")
    public Result<LearningStatsVO> stats() {
        return Result.success(learningStatsService.stats(currentUserId()));
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
