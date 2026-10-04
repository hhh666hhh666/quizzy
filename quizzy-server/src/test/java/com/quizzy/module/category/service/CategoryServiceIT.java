package com.quizzy.module.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分类的**服务集成**测试。
 *
 * <p>分类是**全体用户共用**的一套（CONTEXT.md），所以这里不只验 CRUD，
 * 更该验那条跨表副作用：**删分类会把相关题目的分类引用清空**——
 * 它不是「删掉一条配置」，而是会动到别人的题。
 */
@DisplayName("服务集成 · 分类（共享 + 跨表副作用）")
class CategoryServiceIT extends ApiTestBase {

    @Autowired
    CategoryService categoryService;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    QuestionMapper questionMapper;

    private String uniqueName() {
        return "IT 分类 " + newUsername();
    }

    @Test
    @DisplayName("重名报 409：分类是共享的，名字必须唯一")
    void duplicateNameIsRejected() {
        String name = uniqueName();
        categoryService.create(name, 0);

        assertThatThrownBy(() -> categoryService.create(name, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CONFLICT.getCode());
    }

    @Test
    @DisplayName("删分类：相关题目的分类引用被清空（会动到别人的题）")
    void deleteDetachesQuestions() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();

        Category category = categoryService.create(uniqueName(), 0);

        JsonNode created = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 挂分类的题 " + newUsername(),
                "score", 1,
                "categoryId", category.getId(),
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(created.path("code").asInt()).isZero();
        long questionId = created.path("data").asLong();
        assertThat(questionMapper.selectById(questionId).getCategoryId()).isEqualTo(category.getId());

        categoryService.delete(category.getId());

        // 题目本身还在，只是不再属于任何分类——而不是连带被删
        Question question = questionMapper.selectById(questionId);
        assertThat(question).isNotNull();
        assertThat(question.getCategoryId()).as("分类引用应当被清空").isNull();
    }

    @Test
    @DisplayName("列表按 sort 再按 id 升序（前端的排序依据）")
    void listIsOrderedBySortThenId() {
        Category later = categoryService.create(uniqueName(), 5);
        Category earlier = categoryService.create(uniqueName(), 1);

        List<Long> ids = categoryService.list().stream().map(Category::getId).toList();

        assertThat(ids.indexOf(earlier.getId())).isLessThan(ids.indexOf(later.getId()));
        // 顺手清理：分类是共享的，留着会污染后面用例的列表
        categoryMapper.deleteById(earlier.getId());
        categoryMapper.deleteById(later.getId());
    }
}
