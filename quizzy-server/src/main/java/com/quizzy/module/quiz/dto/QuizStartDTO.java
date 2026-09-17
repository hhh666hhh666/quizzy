package com.quizzy.module.quiz.dto;

import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.quiz.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizStartDTO {

    @NotNull(message = "来源类型不能为空")
    private SourceType sourceType;

    /** sourceType = PAPER 时必填 */
    private Long paperId;

    /** sourceType = QUICK 时必填 */
    private PaperRuleDTO rule;

    /** sourceType = WRONG_BOOK 时可选，默认 20 */
    private Integer count;
}
