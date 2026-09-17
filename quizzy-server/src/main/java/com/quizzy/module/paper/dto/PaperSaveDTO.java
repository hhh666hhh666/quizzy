package com.quizzy.module.paper.dto;

import com.quizzy.module.paper.enums.PaperMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PaperSaveDTO {

    private Long id;

    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "试卷模式不能为空")
    private PaperMode mode;

    /** FIXED 模式必填 */
    private List<Long> questionIds;

    /** RULE 模式必填 */
    private PaperRuleDTO rule;
}
