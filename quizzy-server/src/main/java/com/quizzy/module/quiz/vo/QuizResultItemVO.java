package com.quizzy.module.quiz.vo;

import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.vo.OptionVO;

import java.util.List;

public record QuizResultItemVO(Long questionId,
                               int index,
                               QuestionType type,
                               String stem,
                               Integer score,
                               List<OptionVO> options,
                               List<String> userAnswers,
                               List<String> correctAnswers,
                               boolean answered,
                               boolean correct,
                               /** 收藏 = 在至少一个收藏夹里（见 docs/adr/0030）：结果页据此显示星标 */
                               boolean favorited,
                               String analysis) {
}
