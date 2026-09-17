package com.quizzy.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_stat")
public class QuestionStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Integer answerCount;

    private Integer correctCount;

    private Integer consecutiveCorrect;

    private Integer lastCorrect;

    private Integer inWrongBook;

    private LocalDateTime lastAnswerTime;

    private LocalDateTime updateTime;
}
