package com.quizzy.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptionDTO {

    @NotBlank(message = "选项 label 不能为空")
    private String label;

    @NotBlank(message = "选项内容不能为空")
    private String content;
}
