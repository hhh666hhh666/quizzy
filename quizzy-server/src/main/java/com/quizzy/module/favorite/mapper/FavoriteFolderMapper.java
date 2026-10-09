package com.quizzy.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quizzy.module.favorite.entity.FavoriteFolder;
import com.quizzy.module.favorite.vo.FavoriteFolderVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FavoriteFolderMapper extends BaseMapper<FavoriteFolder> {

    /**
     * 收藏夹列表，带题目数与「最近有新题进来」的时间。
     *
     * <p>⚠️ 这两个统计**不存冗余列**，每次聚合算出来——存一列「最后使用时间」会在删题、
     * 改归属这些路径上慢慢漂掉（docs/adr/0030）。排序同样按它来：刚有新题进来的夹排最前，
     * 空夹（{@code max} 为 null）排在最后，同一批空夹之间按创建时间。
     */
    @Select("""
            select f.id                 as id,
                   f.name               as name,
                   f.intro              as intro,
                   f.is_default         as isDefault,
                   f.is_public          as isPublic,
                   count(fq.question_id) as questionCount,
                   max(fq.create_time)   as lastAddedTime
            from favorite_folder f
            left join favorite_folder_question fq on fq.folder_id = f.id
            where f.user_id = #{userId}
            group by f.id, f.name, f.intro, f.is_default, f.is_public, f.create_time
            order by max(fq.create_time) desc, f.create_time desc
            """)
    List<FavoriteFolderVO> selectFoldersWithStat(@Param("userId") Long userId);
}
