package com.ailearn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "知识点新增/修改参数")
public class KnowledgePointDTO {

    private Long id;

    @NotNull(message = "所属科目不能为空")
    private Long subjectId;

    @Schema(description = "父知识点,0 表示根")
    private Long parentId = 0L;

    @NotBlank(message = "知识点名称不能为空")
    private String name;

    private String description;

    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty = 3;

    private Integer sort = 0;

    private Integer status = 1;
}
