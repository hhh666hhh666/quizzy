package com.quizzy.module.paper.dto;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.util.List;

/**
 * 规则卷的抽题规则，序列化为 JSON 存于 paper.rule_json。
 */
@Data
public class PaperRuleDTO {

    private Long categoryId;

    private List<Long> tagIds;

    private List<QuestionType> types;

    private List<Difficulty> difficulties;

    private Integer count = 20;

    /** 排除最近 N 天内已经作答过的题目，0 或不传表示不排除 */
    private Integer excludeRecentDays;
}
