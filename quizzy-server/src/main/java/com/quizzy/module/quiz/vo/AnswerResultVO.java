package com.quizzy.module.quiz.vo;

import java.util.List;

public record AnswerResultVO(Long questionId,
                             boolean isCorrect,
                             List<String> correctAnswers,
                             String analysis,
                             int score,
                             int obtainedScore,
                             int answeredCount,
                             int questionCount) {
}
