package com.quizzy.module.question.vo;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionListItemVO {

    private Long id;

    private QuestionType type;

    private String stem;

    private Difficulty difficulty;

    private Integer score;

    private String categoryName;

    private Long ownerId;

    private boolean editable;

    private boolean inWrongBook;

    private List<TagVO> tags = new ArrayList<>();
}
