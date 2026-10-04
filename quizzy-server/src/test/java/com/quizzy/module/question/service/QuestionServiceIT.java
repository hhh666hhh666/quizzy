package com.quizzy.module.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.common.BusinessException;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionQueryDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 题目的**服务集成**测试：**事务边界**与**跨表后果**。
 *
 * <p>与接口层的分工：接口层（{@code QuestionApiIT}）验的是 HTTP 契约与越权；
 * 这里验的是「一次删除到底动了几张表」「校验失败会不会留下半成品」——
 * 这些都不体现在响应里，只能查表。
 */
@DisplayName("服务集成 · 题目（事务与跨表）")
class QuestionServiceIT extends ApiTestBase {

    @Autowired
    QuestionService questionService;

    @Autowired
    QuestionStatMapper questionStatMapper;

    @Autowired
    QuestionMapper questionMapper;

    /** 保存一道题，分类用**名字**给（分类没有独立的创建入口）。 */
    private long createQuestionIn(String token, String categoryName) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 分类题 " + newUsername(),
                "score", 1,
                "categoryName", categoryName,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("造题失败：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long createQuestion(String token) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 待删题 " + newUsername(),
                "score", 2,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("造题失败：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long statRows(long questionId) {
        return questionStatMapper.selectCount(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getQuestionId, questionId));
    }

    /** 自己名下可见的题目总数（软删的不算）。 */
    private long visibleCount(long userId) {
        QuestionQueryDTO query = new QuestionQueryDTO();
        query.setPage(1L);
        query.setSize(1L);
        query.setScope("mine");
        // PageResult 是 record，访问器是 total() 而不是 getTotal()
        return questionService.page(query, userId).total();
    }

    private static OptionDTO option(String label) {
        OptionDTO dto = new OptionDTO();
        dto.setLabel(label);
        dto.setContent("选项 " + label);
        return dto;
    }

    @Test
    @DisplayName("分类随题目一起诞生：按名字建出，且同名复用（不会建出两个）")
    void categoryIsBornWithQuestionAndReused() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        String name = "IT 随题而生 " + newUsername();

        long first = createQuestionIn(token, name);
        long second = createQuestionIn(token, name);

        Long firstCategoryId = questionMapper.selectById(first).getCategoryId();
        Long secondCategoryId = questionMapper.selectById(second).getCategoryId();

        assertThat(firstCategoryId).as("分类应当被建出来").isNotNull();
        // 分类是共享的：同一个名字在语义上就是同一个分类——复用，而不是再建一个
        assertThat(secondCategoryId).isEqualTo(firstCategoryId);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("校验失败时不留半成品：题与选项都没插进去")
    void failedSaveLeavesNothingBehind() throws Exception {
        JsonNode me = newAccount();
        long owner = me.path("user").path("id").asLong();

        QuestionSaveDTO dto = new QuestionSaveDTO();
        dto.setType(QuestionType.SINGLE);
        dto.setStem("IT 半成品 " + newUsername());
        dto.setScore(1);
        dto.setAnswers(List.of("A"));
        dto.setOptions(new ArrayList<>(List.of(option("A"), option("B"))));
        // 指向一个不存在的分类：validate() 会在 insert 之前拒绝（QuestionService#validate）
        dto.setCategoryId(999999999L);

        long before = visibleCount(owner);

        assertThatThrownBy(() -> questionService.save(dto, owner))
                .isInstanceOf(BusinessException.class)
                .as("分类不存在应当是 400（BAD_REQUEST）")
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(400);

        // 事务的意义就在这：被拒之后不能留下「有题干没选项」这种残行
        assertThat(visibleCount(owner)).as("校验失败不该多出题目").isEqualTo(before);
    }

    @Test
    @DisplayName("删题：题目软删，但作答统计被硬删（数据模型里记着的代价）")
    void deleteQuestionAlsoDropsStats() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();

        long questionId = createQuestion(token);

        // 直接插一条统计，等价于「答过这道题」——不走答题流程，重点在删除的后果
        QuestionStat stat = new QuestionStat();
        stat.setUserId(owner);
        stat.setQuestionId(questionId);
        stat.setAnswerCount(3);
        stat.setCorrectCount(1);
        stat.setConsecutiveCorrect(0);
        stat.setLastCorrect(0);
        stat.setInWrongBook(1);
        stat.setLastAnswerTime(LocalDateTime.now());
        stat.setUpdateTime(LocalDateTime.now());
        questionStatMapper.insert(stat);
        assertThat(statRows(questionId)).isEqualTo(1);

        long before = visibleCount(owner);
        questionService.delete(questionId, owner);

        // ① 题是软删：自己的题列表里查不到了
        assertThat(visibleCount(owner)).isEqualTo(before - 1);
        // ② 统计是**物理删除**：行真的没了。
        //    ⚠️ 这是 docs/design/数据模型.md 里写明、且刻意保留的代价——
        //    「删掉再重新导入」会连带丢掉这道题的正确率与错题本标记。
        //    写成断言是为了把这个代价钉在明处：谁哪天改成软删，这里会提醒他重新想一遍。
        assertThat(statRows(questionId)).as("作答统计是物理删除").isZero();
    }
}
