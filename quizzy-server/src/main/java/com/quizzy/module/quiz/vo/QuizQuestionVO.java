package com.quizzy.module.quiz.vo;

import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.vo.OptionVO;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionVO {

    private Long questionId;

    private int index;

    private QuestionType type;

    private String stem;

    private Integer score;

    private List<OptionVO> options;

    /** 是否已作答 */
    private boolean answered;

    /** 已作答才有值 */
    private List<String> userAnswers;

    private List<String> correctAnswers;

    private String analysis;

    private Boolean isCorrect;
}
