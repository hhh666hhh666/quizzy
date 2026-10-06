package com.quizzy.module.category.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分类与标签模块的接口测试。
 *
 * <p><b>分类的生命周期与题目绑在一起</b>（原因见
 * {@code docs/todo/archive/2026-10-04-TODO-共享分类缺少归属校验.md}）：
 *
 * <ul>
 *   <li>**没有独立的创建入口**——分类随「保存题目」按名字自动建（同名复用）；
 *   <li>**没有主动删除入口**——一个分类不再被任何题目引用时由系统自动回收。
 * </ul>
 *
 * <p>于是这里验的是这套生命周期，而不是 CRUD。
 */
@DisplayName("接口 · 分类与标签")
class CategoryApiIT extends ApiTestBase {

    /** 分类随「保存题目」一起诞生——传的是**名字**，不是 id。 */
    private long createQuestionIn(String token, String categoryName, String stem) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", stem,
                "score", 1,
                "categoryName", categoryName,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("建题失败，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long categoryIdOf(String token, long questionId) throws Exception {
        return apiGet("/api/questions/" + questionId, token).path("data").path("categoryId").asLong();
    }

    private Long idOfName(String token, String name) throws Exception {
        JsonNode list = apiGet("/api/categories", token);
        assertThat(list.path("code").asInt()).isZero();
        for (JsonNode node : list.path("data")) {
            if (name.equals(node.path("name").asText())) {
                return node.path("id").asLong();
            }
        }
        return null;
    }

    @Test
    @DisplayName("未登录读不到分类与标签")
    void requiresToken() throws Exception {
        assertThat(apiGet("/api/categories", null).path("code").asInt()).isEqualTo(401);
        assertThat(apiGet("/api/tags", null).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("保存题目时按名字建出分类（分类随题目一起诞生）")
    void categoryIsBornWithQuestion() throws Exception {
        String token = newUserToken();
        String name = "IT 分类 " + newUsername();

        long questionId = createQuestionIn(token, name, "IT 挂分类的题 " + newUsername());

        Long categoryId = idOfName(token, name);
        assertThat(categoryId).as("分类应当出现在列表里").isNotNull();
        assertThat(categoryIdOf(token, questionId)).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("同名分类被复用，不会建出两个")
    void sameNameIsReused() throws Exception {
        String token = newUserToken();
        String name = "IT 共用分类 " + newUsername();

        long first = createQuestionIn(token, name, "IT 第一道 " + newUsername());
        long second = createQuestionIn(token, name, "IT 第二道 " + newUsername());

        // 分类是共享的：同一个名字在语义上就是同一个分类
        assertThat(categoryIdOf(token, second)).isEqualTo(categoryIdOf(token, first));
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("改分类名：只迁移我自己的题目，别人的题不动")
    void moveToOnlyTouchesMyOwnQuestions() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        String oldName = "IT 旧名 " + newUsername();
        String newName = "IT 新名 " + newUsername();

        long mine = createQuestionIn(owner, oldName, "IT 我的题 " + newUsername());
        long theirs = createQuestionIn(other, oldName, "IT 别人的题 " + newUsername());
        long oldCategoryId = categoryIdOf(owner, mine);
        assertThat(categoryIdOf(other, theirs)).isEqualTo(oldCategoryId);

        JsonNode res = apiPut("/api/categories/" + oldCategoryId, owner, payload("name", newName));
        assertThat(res.path("code").asInt()).as("改分类名失败，实际响应：%s", res).isZero();

        // ① 我的题迁到了新分类
        assertThat(categoryIdOf(owner, mine)).isEqualTo(idOfName(owner, newName));
        // ② 别人的题**仍挂旧分类**——分类是共享的，我动不了别人的归属
        assertThat(categoryIdOf(other, theirs)).isEqualTo(oldCategoryId);
        // ③ 旧分类还在（它还有别人的引用），不会被自动回收
        assertThat(idOfName(other, oldName)).isEqualTo(oldCategoryId);
    }

    @Test
    @DisplayName("分类名是空白 → 当作没填分类，题目照常保存（不是报错）")
    void blankCategoryNameMeansNoCategory() throws Exception {
        String token = newUserToken();
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 空白分类名 " + newUsername(),
                "score", 1,
                "categoryName", "   ",
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        // 空白 = 没填，与「不传 categoryId」等价；题目有没有分类界面上一眼可见，
        // 静默忽略比把「保存」拒掉更划算。名字超长（>64）才报 400。
        assertThat(res.path("code").asInt()).isZero();

        long questionId = res.path("data").asLong();
        // 字段被 non_null 省略了，所以用 hasNonNull 判
        assertThat(apiGet("/api/questions/" + questionId, token).path("data").hasNonNull("categoryId"))
                .as("空白分类名不该建出分类").isFalse();
    }

    @Test
    @DisplayName("改一个不存在的分类报 404")
    void updateMissingCategory() throws Exception {
        JsonNode res = apiPut("/api/categories/999999999", newUserToken(), payload("name", "随便"));
        assertThat(res.path("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("主动删除的端点已经关闭：删不掉，分类还在")
    void deleteEndpointIsClosed() throws Exception {
        String token = newUserToken();
        String name = "IT 删不掉的 " + newUsername();
        createQuestionIn(token, name, "IT 占着分类 " + newUsername());
        long categoryId = idOfName(token, name);

        apiDelete("/api/categories/" + categoryId, token);

        // 断言**状态**而不是响应码：端点没了，Spring 可能直接给 405 的 HTML 而不是信封体，
        // 而「分类还在」无论走哪条路都成立——这才是要保的东西。
        assertThat(idOfName(token, name)).as("分类不该被删掉").isEqualTo(categoryId);
    }
}
