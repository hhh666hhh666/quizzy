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

    /** 简介；没填时后端不返回（全局 `non_null` 序列化策略），前端按空串处理 */
    private String intro;

    /** 默认收藏夹不可删除、可改名，界面据此决定「删除」按钮是否隐藏 */
    private Boolean isDefault;

    /**
     * 是否公开。
     *
     * <p>⚠️ **目前只是回显**（docs/adr/0032）：公开的收藏夹别人看不到，界面文案不许暗示
     * 「公开后别人能看」。
     */
    private Boolean isPublic;

    private Long questionCount;

    /** 该夹最近一次有新题进来的时间；空夹为 null */
    private LocalDateTime lastAddedTime;
}
