package com.quizzy.module.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quizzy.module.question.entity.Question;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QuestionMapper extends BaseMapper<Question> {

    @Update("update question set category_id = null where category_id = #{categoryId}")
    int clearCategory(@Param("categoryId") Long categoryId);

    @Delete("delete from question_tag where question_id = #{questionId}")
    int deleteTags(@Param("questionId") Long questionId);

    @Insert("insert ignore into question_tag(question_id, tag_id) values(#{questionId}, #{tagId})")
    int insertTag(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    @Select("select tag_id from question_tag where question_id = #{questionId}")
    List<Long> selectTagIds(@Param("questionId") Long questionId);

    @Select("""
            <script>
            select qt.question_id as questionId, qt.tag_id as tagId
            from question_tag qt
            where qt.question_id in
            <foreach collection="questionIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectTagIdsBatch(@Param("questionIds") Collection<Long> questionIds);

    @Select("""
            <script>
            select distinct qt.question_id
            from question_tag qt
            where qt.tag_id in
            <foreach collection="tagIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectQuestionIdsByTagIds(@Param("tagIds") Collection<Long> tagIds);
}
