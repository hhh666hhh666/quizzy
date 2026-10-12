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
                              /** 正确率：整数百分比（0–100，四舍五入）。不为显示精度保留小数——
                               *  PC 与移动端的结果页都直接渲染它，源头取整即两端干净。 */
                              double accuracy,
                              LocalDateTime startTime,
                              LocalDateTime finishTime,
                              List<QuizResultItemVO> items) {
}
