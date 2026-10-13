package com.ailearn.controller;

import com.ailearn.common.Result;
import com.ailearn.dto.KnowledgePointDTO;
import com.ailearn.entity.KnowledgePoint;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.KnowledgePointService;
import com.ailearn.vo.KnowledgePointTreeVO;
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
 * 知识点接口。
 */
@RestController
@RequestMapping("/api/knowledge-points")
@RequiredArgsConstructor
@Tag(name = "知识点管理")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @GetMapping("/tree")
    @Operation(summary = "知识点树(缓存,含题量统计)")
    public Result<List<KnowledgePointTreeVO>> tree(@RequestParam(required = false) Long subjectId) {
        return Result.success(knowledgePointService.tree(subjectId));
    }

    @GetMapping
    @Operation(summary = "知识点平铺列表")
    public Result<List<KnowledgePoint>> list(@RequestParam(required = false) Long subjectId) {
        return Result.success(knowledgePointService.list(subjectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "知识点详情")
    public Result<KnowledgePoint> detail(@PathVariable Long id) {
        return Result.success(knowledgePointService.getById(id));
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "新增知识点")
    public Result<KnowledgePoint> save(@Valid @RequestBody KnowledgePointDTO dto) {
        return Result.success(knowledgePointService.save(dto));
    }

    @PutMapping
    @AdminOnly
    @Operation(summary = "修改知识点")
    public Result<KnowledgePoint> update(@Valid @RequestBody KnowledgePointDTO dto) {
        return Result.success(knowledgePointService.update(dto));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "删除知识点")
    public Result<Void> remove(@PathVariable Long id) {
        knowledgePointService.remove(id);
        return Result.success();
    }
}
