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

    /**
     * 分类**名称**：没有 {@code categoryId} 时按它解析——同名已存在就复用，否则新建。
     *
     * <p>为什么需要它：分类是共享的、且没有独立的创建入口，所以「用户想用一个还不存在的分类」
     * 只能发生在**保存题目**这一刻。放在同一个请求里，分类与题目要么都建出来、要么都回滚。
     * （这与 {@code tags} 的做法一致：标签也是保存题目时按名字自动建。）
     */
    @Size(max = 64, message = "分类名称不超过 64 个字符")
    private String categoryName;

    @Valid
    @NotNull(message = "选项不能为空")
    @Size(min = 2, max = 6, message = "题目至少需要 2 个选项，最多 6 个")
    private List<OptionDTO> options;

    @Size(max = 10, message = "标签最多 10 个")
    private List<String> tags;
}
