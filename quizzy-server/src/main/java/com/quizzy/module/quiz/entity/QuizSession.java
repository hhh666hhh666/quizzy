package com.quizzy.module.quiz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_session")
public class QuizSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long paperId;

    private SourceType sourceType;

    private String title;

    private Integer questionCount;

    private Integer currentIndex;

    private Integer totalScore;

    private Integer obtainedScore;

    private SessionStatus status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
