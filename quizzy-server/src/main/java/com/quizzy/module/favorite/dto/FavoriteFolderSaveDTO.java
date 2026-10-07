package com.quizzy.module.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建 / 改名收藏夹。
 *
 * <p>名字上限 20 字。同一用户内**不许重名**——夹名是主人自己挑东西时的路标，
 * 两个同名夹必然点错（与「昵称允许重名」不冲突，理由见 docs/adr/0030）。
 */
@Data
public class FavoriteFolderSaveDTO {

    @NotBlank(message = "收藏夹名字不能为空")
    @Size(max = 20, message = "收藏夹名字最多 20 个字符")
    private String name;
}
