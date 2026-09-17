package com.quizzy.module.paper.vo;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;

public record QuestionPreviewVO(Long id, QuestionType type, String stem, Difficulty difficulty, Integer score) {
}
