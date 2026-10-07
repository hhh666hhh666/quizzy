package com.quizzy.module.question.vo;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionVO {

    private Long id;

    private QuestionType type;

    private String stem;

    private String analysis;

    private Difficulty difficulty;

    private Integer score;

    private List<String> answers = new ArrayList<>();

    private Long categoryId;

    private String categoryName;

    private Long ownerId;

    private boolean editable;

    private Integer answerCount;

    private Integer correctCount;

    private boolean inWrongBook;

    /** 见 docs/adr/0030：收藏 = 在至少一个收藏夹里。 */
    private boolean favorited;

    private List<OptionVO> options = new ArrayList<>();

    private List<TagVO> tags = new ArrayList<>();
}
