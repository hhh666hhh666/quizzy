package com.quizzy.module.quiz.service;

import com.quizzy.common.util.AnswerUtil;
import com.quizzy.module.question.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 判分策略：单选题与判断题选对即得分；多选题必须与正确答案集合完全一致才得分，
 * 不做部分分、不做倒扣分。题目分值由建题时指定。
 */
@Component
public class ScoreStrategy {

    public record JudgeResult(boolean correct, int score) {
    }

    public JudgeResult judge(QuestionType type, String correctAnswer, String userAnswer, Integer questionScore) {
        int score = questionScore == null || questionScore <= 0 ? 1 : questionScore;
        List<String> userAnswers = AnswerUtil.split(userAnswer);
        if (userAnswers.isEmpty()) {
            return new JudgeResult(false, 0);
        }
        boolean correct = AnswerUtil.same(correctAnswer, userAnswer);
        if (correct && type == QuestionType.MULTI && userAnswers.size() < AnswerUtil.split(correctAnswer).size()) {
            // 少选同样视为不完全正确
            correct = false;
        }
        return new JudgeResult(correct, correct ? score : 0);
    }
}
