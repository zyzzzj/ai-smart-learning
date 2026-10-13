package com.ailearn.controller;

import com.ailearn.common.BusinessException;
import com.ailearn.common.PageResult;
import com.ailearn.common.Result;
import com.ailearn.common.UserContext;
import com.ailearn.dto.ExamAutoComposeDTO;
import com.ailearn.dto.ExamDTO;
import com.ailearn.dto.ExamSubmitDTO;
import com.ailearn.entity.Exam;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.ExamService;
import com.ailearn.vo.ExamPaperVO;
import com.ailearn.vo.ExamResultVO;
import com.ailearn.vo.ExamVO;
import com.ailearn.vo.ScoreRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能考试接口。
 */
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Tag(name = "智能考试", description = "组卷、考试、自动判分、AI 评分、成绩排行")
public class ExamController {

    private final ExamService examService;

    /* ---------------- 学生端 ---------------- */

    @GetMapping
    @Operation(summary = "可参加的考试列表(学生)")
    public Result<PageResult<ExamVO>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(examService.page(keyword, null, page, size, UserContext.getUserId(), false));
    }

    @PostMapping("/{examId}/start")
    @Operation(summary = "开始考试(返回试卷,不含答案)")
    public Result<ExamPaperVO> start(@PathVariable Long examId) {
        return Result.success(examService.start(examId, currentUserId()));
    }

    @PostMapping("/{examId}/save")
    @Operation(summary = "暂存单题作答(可反复调用,便于超时自动交卷)")
    public Result<Void> save(@PathVariable Long examId,
                             @RequestParam Long questionId,
                             @RequestParam(required = false) String userAnswer) {
        examService.saveAnswer(examId, questionId, userAnswer, currentUserId());
        return Result.success();
    }

    @PostMapping("/{examId}/submit")
    @Operation(summary = "交卷并判分(客观题自动判分,主观题 AI 评分)")
    public Result<ExamResultVO> submit(@PathVariable Long examId, @RequestBody(required = false) ExamSubmitDTO dto) {
        return Result.success(examService.submit(examId, dto == null ? new ExamSubmitDTO() : dto, currentUserId()));
    }

    @GetMapping("/{examId}/result")
    @Operation(summary = "我的考试成绩(含逐题解析与 AI 评语)")
    public Result<ExamResultVO> result(@PathVariable Long examId) {
        return Result.success(examService.myResult(examId, currentUserId()));
    }

    @GetMapping("/{examId}/rank")
    @Operation(summary = "成绩排行")
    public Result<List<ScoreRankVO>> rank(@PathVariable Long examId,
                                          @RequestParam(defaultValue = "20") int limit) {
        return Result.success(examService.rank(examId, limit));
    }

    /* ---------------- 管理端 ---------------- */

    @GetMapping("/manage")
    @AdminOnly
    @Operation(summary = "考试管理列表")
    public Result<PageResult<ExamVO>> manageList(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.success(examService.page(keyword, status, page, size, UserContext.getUserId(), true));
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "手动组卷创建考试")
    public Result<Exam> create(@Valid @RequestBody ExamDTO dto) {
        return Result.success(examService.create(dto, UserContext.getUserId()));
    }

    @PostMapping("/auto-compose")
    @AdminOnly
    @Operation(summary = "AI 自动组卷(按题型/难度配比抽题)")
    public Result<Exam> autoCompose(@Valid @RequestBody ExamAutoComposeDTO dto) {
        return Result.success(examService.autoCompose(dto, UserContext.getUserId()));
    }

    @PutMapping
    @AdminOnly
    @Operation(summary = "修改考试(仅草稿)")
    public Result<Exam> update(@Valid @RequestBody ExamDTO dto) {
        return Result.success(examService.update(dto));
    }

    @PutMapping("/{examId}/status/{status}")
    @AdminOnly
    @Operation(summary = "发布/关闭考试(DRAFT/PUBLISHED/CLOSED)")
    public Result<Void> changeStatus(@PathVariable Long examId, @PathVariable String status) {
        examService.changeStatus(examId, status);
        return Result.success();
    }

    @DeleteMapping("/{examId}")
    @AdminOnly
    @Operation(summary = "删除考试")
    public Result<Void> remove(@PathVariable Long examId) {
        examService.remove(examId);
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
