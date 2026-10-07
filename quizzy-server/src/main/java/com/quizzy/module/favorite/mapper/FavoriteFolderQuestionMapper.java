package com.quizzy.module.favorite.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 收藏夹 ↔ 题目的关联表。用裸 SQL 而不是 MyBatis-Plus 的实体映射，原因有二：
 *
 * <ol>
 *   <li>这张表是**复合主键**（{@code folder_id} + {@code question_id}），没有单列主键，
 *       而这里的操作全是「按条件批量删」和「按条件取 id」，用不到实体映射；
 *   <li>几条删除必须绕过默认作用域一次性删干净（删题要清掉**所有人**对它的收藏），
 *       写成显式 SQL 比包一层更看得清。
 * </ol>
 *
 * <p>⚠️ 顺序上它们都只是「关联」，题目本身不受影响：把关联删光不等于删题（docs/adr/0030）。
 */
public interface FavoriteFolderQuestionMapper {

    /** 加进某个夹；已在里面就什么也不做（重复点星标不该报错）。 */
    @Insert("insert ignore into favorite_folder_question(folder_id, question_id) values(#{folderId}, #{questionId})")
    int insertIgnore(@Param("folderId") Long folderId, @Param("questionId") Long questionId);

    /** 取消收藏：把这道题从该用户的**所有**夹里移出。 */
    @Delete("delete from favorite_folder_question where question_id = #{questionId} "
            + "and folder_id in (select id from favorite_folder where user_id = #{userId})")
    int deleteByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 从**某一个**夹里移出（题若因此不属于任何夹，就不再是收藏）。 */
    @Delete("delete from favorite_folder_question where folder_id = #{folderId} and question_id = #{questionId}")
    int deleteByFolderAndQuestion(@Param("folderId") Long folderId, @Param("questionId") Long questionId);

    /** 删夹时清空它的关联。 */
    @Delete("delete from favorite_folder_question where folder_id = #{folderId}")
    int deleteByFolder(@Param("folderId") Long folderId);

    /**
     * 删题时把这道题从**所有人**的收藏里摘掉。
     *
     * <p>与题目的「删题硬删作答统计」保持一致：不留指向已删题目的悬挂行（docs/adr/0029）。
     */
    @Delete("delete from favorite_folder_question where question_id = #{questionId}")
    int deleteByQuestion(@Param("questionId") Long questionId);

    /** 注销账号：清空他所有夹里的关联，再删夹本身。 */
    @Delete("delete from favorite_folder_question "
            + "where folder_id in (select id from favorite_folder where user_id = #{userId})")
    int deleteByUser(@Param("userId") Long userId);

    /**
     * 注销账号：清掉别人在他题目上的收藏。
     *
     * <p>理论上这是空集（别人看不到、也引用不到他的私有题），留着这一步是为了**不依赖那条推理**。
     */
    @Delete("delete from favorite_folder_question "
            + "where question_id in (select id from question where owner_id = #{ownerId})")
    int deleteByOwnedQuestions(@Param("ownerId") Long ownerId);

    /**
     * 这一页题目里哪些被收藏过——列表打标记用。
     *
     * <p>⚠️ 刻意只查**当前页**那几十个 id，而不是像「只看错题」那样把该用户收藏的题全捞出来：
     * 打标记是每次翻页都要走的热路径，收窄到本页能省掉一次随收藏量增长的全量查询。
     */
    @Select("""
            <script>
            select distinct fq.question_id
            from favorite_folder_question fq
            join favorite_folder f on f.id = fq.folder_id
            where f.user_id = #{userId}
              and fq.question_id in
              <foreach collection="questionIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectFavoriteQuestionIds(@Param("userId") Long userId,
                                         @Param("questionIds") Collection<Long> questionIds);

    /** 这个用户收藏的全部题目 id——「只看收藏」筛选用（与「只看错题」同一套做法）。 */
    @Select("select distinct fq.question_id from favorite_folder_question fq "
            + "join favorite_folder f on f.id = fq.folder_id where f.user_id = #{userId}")
    List<Long> selectAllFavoriteQuestionIds(@Param("userId") Long userId);

    /** 按一个或多个夹取题目 id——「按收藏夹筛选」用。 */
    @Select("""
            <script>
            select distinct fq.question_id
            from favorite_folder_question fq
            join favorite_folder f on f.id = fq.folder_id
            where f.user_id = #{userId}
              and fq.folder_id in
              <foreach collection="folderIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectQuestionIdsByFolders(@Param("userId") Long userId,
                                          @Param("folderIds") Collection<Long> folderIds);

    /** 这道题在这个用户的哪些夹里——「修改收藏夹」面板与撤销都要用它。 */
    @Select("select fq.folder_id from favorite_folder_question fq "
            + "join favorite_folder f on f.id = fq.folder_id "
            + "where f.user_id = #{userId} and fq.question_id = #{questionId}")
    List<Long> selectFolderIdsByQuestion(@Param("userId") Long userId,
                                         @Param("questionId") Long questionId);

    /**
     * 某个夹里的题目 id，按进夹时间倒序——开练习时抽题用。
     *
     * <p>⚠️ 刻意带上 {@code userId} 一起过滤：只按 {@code folderId} 查的话，猜一个 id 就能把
     * **别人**夹里的题拉进自己的练习会话。这里从 SQL 层堵死，不依赖调用方先做过归属校验。
     */
    @Select("select fq.question_id from favorite_folder_question fq "
            + "join favorite_folder f on f.id = fq.folder_id "
            + "where fq.folder_id = #{folderId} and f.user_id = #{userId} "
            + "order by fq.create_time desc limit #{limit}")
    List<Long> selectQuestionIdsByFolder(@Param("userId") Long userId,
                                         @Param("folderId") Long folderId,
                                         @Param("limit") int limit);

    /**
     * 这个用户收藏的全部题目 id，按进夹时间倒序——「全部收藏」开练习时抽题用。
     *
     * <p>一道题在多个夹里会出现多次，**去重交给调用方**（在 Java 里 distinct 能保住顺序），
     * 这里不写 {@code distinct} 是因为 MySQL 下它与 {@code order by fq.create_time} 不兼容。
     */
    @Select("select fq.question_id from favorite_folder_question fq "
            + "join favorite_folder f on f.id = fq.folder_id "
            + "where f.user_id = #{userId} order by fq.create_time desc limit #{limit}")
    List<Long> selectQuestionIdsByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
