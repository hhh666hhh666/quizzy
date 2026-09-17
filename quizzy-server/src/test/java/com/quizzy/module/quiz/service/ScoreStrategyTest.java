package com.quizzy.module.quiz.service;

import com.quizzy.module.question.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreStrategyTest {

    private final ScoreStrategy scoreStrategy = new ScoreStrategy();

    @Test
    @DisplayName("单选题选对得满分，选错得 0 分")
    void singleChoice() {
        assertTrue(scoreStrategy.judge(QuestionType.SINGLE, "B", "B", 2).correct());
        assertEquals(2, scoreStrategy.judge(QuestionType.SINGLE, "B", "B", 2).score());
        assertFalse(scoreStrategy.judge(QuestionType.SINGLE, "B", "C", 2).correct());
        assertEquals(0, scoreStrategy.judge(QuestionType.SINGLE, "B", "C", 2).score());
    }

    @Test
    @DisplayName("多选题必须全对，顺序不影响判定，少选或多选均不得分")
    void multiChoice() {
        assertTrue(scoreStrategy.judge(QuestionType.MULTI, "A,C", "C,A", 5).correct());
        assertEquals(5, scoreStrategy.judge(QuestionType.MULTI, "A,C", "C,A", 5).score());

        assertFalse(scoreStrategy.judge(QuestionType.MULTI, "A,C", "A", 5).correct());
        assertFalse(scoreStrategy.judge(QuestionType.MULTI, "A,C", "A,C,D", 5).correct());
        assertEquals(0, scoreStrategy.judge(QuestionType.MULTI, "A,C", "A,C,D", 5).score());
    }

    @Test
    @DisplayName("未作答不计分")
    void unanswered() {
        ScoreStrategy.JudgeResult result = scoreStrategy.judge(QuestionType.SINGLE, "A", "", 3);
        assertFalse(result.correct());
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("分值为空时降级为 1 分")
    void defaultScore() {
        assertEquals(1, scoreStrategy.judge(QuestionType.JUDGE, "A", "A", null).score());
    }
}
