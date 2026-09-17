package com.quizzy.module.question.dto;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.util.List;

@Data
public class QuestionQueryDTO {

    private Long page = 1L;

    private Long size = 10L;

    private String keyword;

    private QuestionType type;

    private Difficulty difficulty;

    private Long categoryId;

    private List<Long> tagIds;

    /** all | mine | public */
    private String scope;

    /** 只查看错题本中的题目 */
    private Boolean onlyWrong;
}
