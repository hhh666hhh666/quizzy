package com.quizzy.module.favorite.dto;

import lombok.Data;

import java.util.List;

/**
 * 覆盖式设置「这道题属于哪些收藏夹」。
 *
 * <p>⚠️ {@code folderIds} **为空 = 留在默认收藏夹**，不是取消收藏——一次手滑不该丢收藏（docs/adr/0030）。
 * 要取消收藏请用「取消收藏」那个动作。
 */
@Data
public class FavoriteFolderIdsDTO {

    private List<Long> folderIds;
}
