package com.quizzy.module.favorite.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 把一批题目**加入**某个收藏夹（只加不减）。
 *
 * <p>只用于题库页的批量条与「修改收藏夹」面板的批量入口。单题那种「覆盖式设置」
 * 走 {@link FavoriteFolderIdsDTO}，两者的差别见 docs/adr/0030。
 */
@Data
public class FavoriteQuestionsDTO {

    @NotEmpty(message = "请先选题目")
    private List<Long> questionIds;
}
