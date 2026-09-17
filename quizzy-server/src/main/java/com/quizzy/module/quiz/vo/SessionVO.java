package com.quizzy.module.quiz.vo;

import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SessionVO {

    private Long id;

    private String title;

    private SourceType sourceType;

    private SessionStatus status;

    private Long paperId;

    private int questionCount;

    private int currentIndex;

    private int totalScore;

    private int obtainedScore;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private List<QuizQuestionVO> questions = new ArrayList<>();
}
