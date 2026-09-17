package com.quizzy.module.quiz.vo;

import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record SessionResultVO(Long id,
                              String title,
                              SourceType sourceType,
                              SessionStatus status,
                              int questionCount,
                              int answeredCount,
                              int correctCount,
                              int unansweredCount,
                              int totalScore,
                              int obtainedScore,
                              double accuracy,
                              LocalDateTime startTime,
                              LocalDateTime finishTime,
                              List<QuizResultItemVO> items) {
}
