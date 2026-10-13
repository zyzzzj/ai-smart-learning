package com.ailearn.controller;

import com.ailearn.common.PageResult;
import com.ailearn.common.Result;
import com.ailearn.dto.QuestionDTO;
import com.ailearn.dto.QuestionQueryDTO;
import com.ailearn.entity.Question;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.HotQuestionService;
import com.ailearn.service.QuestionService;
import com.ailearn.vo.HotQuestionVO;
import com.ailearn.vo.QuestionDetailVO;
import com.ailearn.vo.QuestionVO;
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
 * 题库接口。
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "题库管理")
public class QuestionController {

    private final QuestionService questionService;
    private final HotQuestionService hotQuestionService;

    @GetMapping
    @Operation(summary = "题目分页查询(科目/知识点/题型/难度/来源/关键词)")
    public Result<PageResult<QuestionVO>> page(QuestionQueryDTO query) {
        return Result.success(questionService.page(query));
    }

    @GetMapping("/hot")
    @Operation(summary = "热门题目 TopN(Redis ZSet 排行,带缓存)")
    public Result<List<HotQuestionVO>> hot(@RequestParam(required = false) Long subjectId,
                                           @RequestParam(defaultValue = "10") int limit) {
        return Result.success(hotQuestionService.topN(subjectId, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "题目详情(含答案与解析,累计热度)")
    public Result<QuestionDetailVO> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(id));
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "新增题目")
    public Result<Question> create(@Valid @RequestBody QuestionDTO dto) {
        return Result.success(questionService.create(dto));
    }

    @PutMapping
    @AdminOnly
    @Operation(summary = "修改题目")
    public Result<Question> update(@Valid @RequestBody QuestionDTO dto) {
        return Result.success(questionService.update(dto));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "删除题目")
    public Result<Void> remove(@PathVariable Long id) {
        questionService.remove(id);
        return Result.success();
    }

    @DeleteMapping
    @AdminOnly
    @Operation(summary = "批量删除题目")
    public Result<Void> removeBatch(@RequestParam List<Long> ids) {
        questionService.removeBatch(ids);
        return Result.success();
    }
}
