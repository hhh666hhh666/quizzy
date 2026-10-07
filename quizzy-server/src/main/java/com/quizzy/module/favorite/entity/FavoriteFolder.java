package com.quizzy.module.favorite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一个具名收藏夹。定义与取舍见 docs/adr/0030。
 *
 * <p>{@code isDefault} 为 1 的那个是用户的**默认收藏夹**：点星标时题目先落到它里面。
 * 它不可删除、可以改名——所以它的名字**不许写死进界面文案**。
 */
@Data
@TableName("favorite_folder")
public class FavoriteFolder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** 1 = 默认收藏夹 */
    private Integer isDefault;

    private LocalDateTime createTime;
}
