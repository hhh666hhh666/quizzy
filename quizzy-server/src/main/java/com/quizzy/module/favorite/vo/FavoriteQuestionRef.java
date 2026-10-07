package com.quizzy.module.favorite.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 「某道题在某个桶里」的引用：题目 id + 它最近一次被收进这个桶的时间。
 *
 * <p>⚠️ 这是**关联表上的事实**，不是题目自身的字段——所以它归收藏模块，而不是塞进题目实体。
 * 列表项上那个 {@code favoritedAt} 由收藏模块在组装完之后回填。
 *
 * <p>不是接口的响应体（收藏夹列表返回的仍是题目列表项），所以名字不叫 VO。
 */
@Data
public class FavoriteQuestionRef {

    private Long questionId;

    /** 最近一次进这个桶的时间；「全部收藏」桶里取的是跨夹的最大值 */
    private LocalDateTime favoritedAt;
}
