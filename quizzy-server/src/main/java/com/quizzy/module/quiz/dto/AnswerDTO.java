package com.quizzy.module.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;

    /** 用户作答内容，逗号分隔的 label；为空字符串表示清空作答 */
    private String answer;
}
