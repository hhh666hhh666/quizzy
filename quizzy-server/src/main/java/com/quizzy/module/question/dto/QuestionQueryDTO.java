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

    /**
     * 分类筛选，可多选。
     *
     * <p>⚠️ 与 {@link #uncategorized} 是**或**的关系：勾了「MySQL」与「未分类」，
     * 两类题都会返回。分类本身是单选归属（一道题最多归入一个分类），多选表达的是
     * 「这几个分类里的都行」。
     */
    private List<Long> categoryIds;

    /** 是否包含「未分类」（{@code category_id} 为空）的题目 */
    private Boolean uncategorized;

    private List<Long> tagIds;

    /** all | mine | public */
    private String scope;

    /** 只查看错题本中的题目 */
    private Boolean onlyWrong;
}
