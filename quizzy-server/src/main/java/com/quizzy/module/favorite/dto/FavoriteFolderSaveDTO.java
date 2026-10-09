package com.quizzy.module.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建 / 编辑收藏夹。
 *
 * <p>名字上限 20 字。同一用户内**不许重名**——夹名是主人自己挑东西时的路标，
 * 两个同名夹必然点错（与「昵称允许重名」不冲突，理由见 docs/adr/0030）。
 *
 * <p>⚠️ `intro` / `isPublic` 两个字段**都不加 `@NotNull`**：创建走的是同一个接口，
 * 而「只是想建个夹」的调用方（含存量客户端与既有测试）只会传 `name`——加了必填就会 400。
 * 缺省即默认值：没给 `intro` 就是不填，没给 `isPublic` 就是不公开。理由见 docs/adr/0032。
 */
@Data
public class FavoriteFolderSaveDTO {

    @NotBlank(message = "收藏夹名字不能为空")
    @Size(max = 20, message = "收藏夹名字最多 20 个字符")
    private String name;

    /** 简介，最多 200 字；不传 / 传空串都表示没填 */
    @Size(max = 200, message = "收藏夹简介最多 200 个字符")
    private String intro;

    /** 是否公开；不传按 false 处理（⚠️ 目前仅存不用，见 docs/adr/0032） */
    private Boolean isPublic;
}
