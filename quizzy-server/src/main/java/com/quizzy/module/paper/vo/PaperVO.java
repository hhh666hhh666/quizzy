package com.quizzy.module.paper.vo;

import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.paper.enums.PaperMode;
import lombok.Data;

import java.util.List;

@Data
public class PaperVO {

    private Long id;

    private String title;

    private String description;

    private PaperMode mode;

    private PaperRuleDTO rule;

    private Integer questionCount;

    private List<Long> questionIds;
}
