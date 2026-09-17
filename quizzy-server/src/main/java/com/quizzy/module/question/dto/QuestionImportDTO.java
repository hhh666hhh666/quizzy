package com.quizzy.module.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * JSON 批量导入的题目结构。
 */
@Data
public class QuestionImportDTO {

    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    @Valid
    private List<OptionDTO> options;

    private List<String> answer;

    private String analysis;

    private String difficulty;

    private Integer score;

    private String category;

    private List<String> tags;
}
