package com.quizzy.module.question.dto;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionSaveDTO {

    /** 为空表示新建 */
    private Long id;

    @NotNull(message = "题型不能为空")
    private QuestionType type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    private String analysis;

    private Difficulty difficulty;

    @NotNull(message = "正确答案不能为空")
    @Size(min = 1, message = "正确答案不能为空")
    private List<String> answers;

    @Min(value = 1, message = "分值至少为 1")
    @Max(value = 100, message = "分值最多为 100")
    private Integer score;

    private Long categoryId;

    @Valid
    @NotNull(message = "选项不能为空")
    @Size(min = 2, max = 6, message = "题目至少需要 2 个选项，最多 6 个")
    private List<OptionDTO> options;

    @Size(max = 10, message = "标签最多 10 个")
    private List<String> tags;
}
