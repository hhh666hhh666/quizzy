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
 * <p>分类是**全体用户共用**的一套（见 CONTEXT.md），所以这里既钉契约，也钉一条删除的副作用——
 * 「删分类会把题目的分类清空」是设计好的行为，不是顺手改的，写下来免得以后被人当成 bug 修掉。
 */
@DisplayName("接口 · 分类与标签")
class CategoryApiIT extends ApiTestBase {

    private long createCategory(String token, String name) throws Exception {
        JsonNode res = apiPost("/api/categories", token, payload("name", name, "sort", 0));
        assertThat(res.path("code").asInt()).as("建分类应当成功，实际响应：%s", res).isZero();
        return res.path("data").path("id").asLong();
    }

    private static List<String> names(JsonNode list) {
        List<String> names = new ArrayList<>();
        list.forEach(node -> names.add(node.path("name").asText()));
        return names;
    }

    @Test
    @DisplayName("未登录读不到分类与标签")
    void requiresToken() throws Exception {
        assertThat(apiGet("/api/categories", null).path("code").asInt()).isEqualTo(401);
        assertThat(apiGet("/api/tags", null).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("新建分类出现在列表里；重名报 409")
    void createAndDuplicate() throws Exception {
        String token = newUserToken();
        String name = "IT 分类 " + newUsername();
        long id = createCategory(token, name);

        JsonNode list = apiGet("/api/categories", token);
        assertThat(list.path("code").asInt()).isZero();
        assertThat(names(list.path("data"))).contains(name);

        JsonNode again = apiPost("/api/categories", token, payload("name", name, "sort", 0));
        assertThat(again.path("code").asInt()).isEqualTo(409);
        assertThat(again.path("message").asText()).isEqualTo("分类已存在");

        // 重名被挡之后，那个 id 仍然只对应一条记录。
        assertThat(apiPut("/api/categories/" + id, token, payload("name", name, "sort", 1))
                .path("code").asInt()).isZero();
    }

    @Test
    @DisplayName("名称非法（空白）报 400")
    void blankNameIsRejected() throws Exception {
        JsonNode res = apiPost("/api/categories", newUserToken(), payload("name", "   ", "sort", 0));
        assertThat(res.path("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("删分类会把该分类下题目的分类清空——这是设计行为，不是 bug")
    void deleteCategoryDetachesQuestions() throws Exception {
        String token = newUserToken();
        long categoryId = createCategory(token, "IT 待删分类 " + newUsername());

        JsonNode created = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 挂分类的题 " + newUsername(),
                "difficulty", "EASY",
                "score", 1,
                "categoryId", categoryId,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(created.path("code").asInt()).isZero();
        long questionId = created.path("data").asLong();

        assertThat(apiGet("/api/questions/" + questionId, token).path("data").path("categoryId").asLong())
                .isEqualTo(categoryId);

        assertThat(apiDelete("/api/categories/" + categoryId, token).path("code").asInt()).isZero();

        // 题目本身还在，只是不再属于任何分类——而不是连带被删。
        JsonNode after = apiGet("/api/questions/" + questionId, token);
        assertThat(after.path("code").asInt()).isZero();
        // ⚠️ 不能用 path("categoryId").isNull()：接口配了 `non_null`，被清空后这个字段
        //    直接不出现，拿到的会是 MissingNode，而 MissingNode.isNull() 是 false。
        assertThat(after.path("data").hasNonNull("categoryId")).as("分类应被清空").isFalse();
    }

    @Test
    @DisplayName("改一个不存在的分类报 404")
    void updateMissingCategory() throws Exception {
        JsonNode res = apiPut("/api/categories/999999999", newUserToken(), payload("name", "随便", "sort", 0));
        assertThat(res.path("code").asInt()).isEqualTo(404);
    }
}
