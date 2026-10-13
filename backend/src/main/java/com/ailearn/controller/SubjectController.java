package com.ailearn.controller;

import com.ailearn.common.Result;
import com.ailearn.entity.Subject;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 科目 / 课程接口。
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "科目管理")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "启用中的科目列表(缓存)")
    public Result<List<Subject>> list() {
        return Result.success(subjectService.listEnabled());
    }

    @GetMapping("/all")
    @AdminOnly
    @Operation(summary = "全部科目(管理端)")
    public Result<List<Subject>> listAll() {
        return Result.success(subjectService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "科目详情")
    public Result<Subject> detail(@PathVariable Long id) {
        return Result.success(subjectService.getById(id));
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "新增科目")
    public Result<Subject> save(@RequestBody Subject subject) {
        return Result.success(subjectService.save(subject));
    }

    @PutMapping
    @AdminOnly
    @Operation(summary = "修改科目")
    public Result<Subject> update(@RequestBody Subject subject) {
        return Result.success(subjectService.update(subject));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "删除科目")
    public Result<Void> remove(@PathVariable Long id) {
        subjectService.remove(id);
        return Result.success();
    }
}
