package com.quizzy.module.favorite.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹列表项：夹本身的信息 + 从关联表聚合出来的统计。
 *
 * <p>{@code questionCount} 与 {@code lastAddedTime} 都是**算出来的**，不是表里的列——见 docs/adr/0030。
 * 夹列表按 {@code lastAddedTime} 倒序（刚有新题进来的排最前），空夹排在最后。
 */
@Data
public class FavoriteFolderVO {

    private Long id;

    private String name;

    /** 默认收藏夹不可删除、可改名，界面据此决定「删除」按钮是否隐藏 */
    private Boolean isDefault;

    private Long questionCount;

    /** 该夹最近一次有新题进来的时间；空夹为 null */
    private LocalDateTime lastAddedTime;
}
