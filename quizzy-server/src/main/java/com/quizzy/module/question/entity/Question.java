package com.quizzy.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private QuestionType type;

    private String stem;

    private String analysis;

    private Difficulty difficulty;

    /** 正确答案选项 label，多个用逗号分隔，如 A,C */
    private String answer;

    private Integer score;

    private Long categoryId;

    /** NULL 表示公开题（所有人可读，只读） */
    private Long ownerId;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
