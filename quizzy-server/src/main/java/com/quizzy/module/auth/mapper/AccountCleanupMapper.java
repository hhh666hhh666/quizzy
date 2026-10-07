package com.quizzy.module.auth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 注销账号时的数据清理。取舍见 docs/adr/0029。
 *
 * <p><b>为什么是一组裸 SQL，而不是复用各模块的 Mapper / Service：</b>
 *
 * <ol>
 *   <li><b>没有任何外键</b>，所以没有数据库级级联可用，清理顺序必须自己保证；
 *   <li>`question` 带 {@code @TableLogic}，走 MyBatis-Plus 的 delete 只会**软删**——
 *       而注销要的是真删（理由见 ADR 0029），必须绕过它；
 *   <li>用子查询按归属一次性删，避免「先查 id 列表、再拼 IN」在列表为空时拼出非法 SQL。
 * </ol>
 *
 * <p>⚠️ 顺序不能随便调：先删引用方（作答、关联表、子表），再删被引用方（题目、试卷），最后删账号。
 */
public interface AccountCleanupMapper {

    /** 他的所有作答记录。 */
    @Delete("delete from quiz_answer where session_id in (select id from quiz_session where user_id = #{userId})")
    int deleteAnswers(@Param("userId") Long userId);

    /** 他的所有会话。 */
    @Delete("delete from quiz_session where user_id = #{userId}")
    int deleteSessions(@Param("userId") Long userId);

    /** 他的作答统计（错题本就是从这里投影出来的）。 */
    @Delete("delete from question_stat where user_id = #{userId}")
    int deleteStats(@Param("userId") Long userId);

    /** 他那批试卷的题目列表。 */
    @Delete("delete from paper_question where paper_id in (select id from paper where owner_id = #{ownerId})")
    int deletePaperQuestions(@Param("ownerId") Long ownerId);

    /** 他的试卷。 */
    @Delete("delete from paper where owner_id = #{ownerId}")
    int deletePapers(@Param("ownerId") Long ownerId);

    /** 他题目的标签关联。 */
    @Delete("delete from question_tag where question_id in (select id from question where owner_id = #{ownerId})")
    int deleteQuestionTags(@Param("ownerId") Long ownerId);

    /** 他题目的选项。 */
    @Delete("delete from question_option where question_id in (select id from question where owner_id = #{ownerId})")
    int deleteQuestionOptions(@Param("ownerId") Long ownerId);

    /**
     * 别人在他题目上留下的统计。
     *
     * <p>理论上这是空集——别人看不到、也引用不到他的私有题（见 ADR 0029）。留着这一步是为了
     * **不依赖那条推理**：真有脏数据时不至于留下指向已删题目的统计行。
     */
    @Delete("delete from question_stat where question_id in (select id from question where owner_id = #{ownerId})")
    int deleteStatsOfOwnedQuestions(@Param("ownerId") Long ownerId);

    /** 他的题目（**物理删**，含已被软删的那些）。 */
    @Delete("delete from question where owner_id = #{ownerId}")
    int deleteQuestions(@Param("ownerId") Long ownerId);

    /** 最后删账号本身。 */
    @Delete("delete from `user` where id = #{userId}")
    int deleteUser(@Param("userId") Long userId);

    /** 他的收藏关联（要排在删收藏夹之前）。 */
    @Delete("delete from favorite_folder_question "
            + "where folder_id in (select id from favorite_folder where user_id = #{userId})")
    int deleteFavoriteLinks(@Param("userId") Long userId);

    /** 别人在他题目上的收藏（要排在删题目之前）。理由同 {@link #deleteStatsOfOwnedQuestions}。 */
    @Delete("delete from favorite_folder_question "
            + "where question_id in (select id from question where owner_id = #{ownerId})")
    int deleteFavoriteLinksOfOwnedQuestions(@Param("ownerId") Long ownerId);

    /** 他的收藏夹。 */
    @Delete("delete from favorite_folder where user_id = #{userId}")
    int deleteFavoriteFolders(@Param("userId") Long userId);
}
