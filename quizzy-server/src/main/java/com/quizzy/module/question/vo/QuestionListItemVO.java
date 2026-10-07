package com.quizzy.module.question.vo;

import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionListItemVO {

    private Long id;

    private QuestionType type;

    private String stem;

    private Difficulty difficulty;

    private Integer score;

    private String categoryName;

    private Long ownerId;

    private boolean editable;

    private boolean inWrongBook;

    /** 这道题是不是已在某个收藏夹里（等价于「收藏过」）。见 docs/adr/0030。 */
    private boolean favorited;

    /**
     * 什么时候被收藏的（最近一次进夹的时间）。
     *
     * <p>⚠️ **只有收藏夹列表会填这个字段**——它是「某个桶」里的时间，而题目列表根本没有桶的概念
     * （同一道题在不同夹里时间不同）。题库列表下发它会是 null，而 {@code non_null} 序列化下
     * null 字段不出现在响应里，所以前端把它标成可选。
     */
    private LocalDateTime favoritedAt;

    private List<TagVO> tags = new ArrayList<>();
}
