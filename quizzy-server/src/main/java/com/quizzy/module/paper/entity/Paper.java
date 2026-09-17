package com.quizzy.module.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.quizzy.module.paper.enums.PaperMode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private PaperMode mode;

    private String ruleJson;

    private Integer questionCount;

    private Long ownerId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
