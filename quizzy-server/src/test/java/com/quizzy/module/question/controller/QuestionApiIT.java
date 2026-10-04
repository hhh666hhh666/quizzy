package com.quizzy.module.question.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 题目模块的接口测试：契约（分页信封）+ **数据边界**。
 *
 * <p>数据边界这一半才是本层独有的价值——它复现不了用户操作，但能直接构造「拿别人的 id 来改」，
 * 而那正是前端界面上根本点不出来的路径。
 */
@DisplayName("接口 · 题目")
class QuestionApiIT extends ApiTestBase {

    /** 造一道合法单选题，返回 id。库是共享的，题干带上随机标识以便认领。 */
    private long createSingle(String token, String stem) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", stem,
                "analysis", "IT 造的解析",
                "difficulty", "EASY",
                "score", 5,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "选项 A"),
                        payload("label", "B", "content", "选项 B"))));
        assertThat(res.path("code").asInt()).as("建题应当成功，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    /** 一份「能通过校验」的请求体——用于那些要走到归属校验、而不是先被参数校验拦下的用例。 */
    private static Object validBody(String stem) {
        return payload(
                "type", "SINGLE", "stem", stem, "score", 1, "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B")));
    }

    private static List<Long> ids(JsonNode list) {
        List<Long> ids = new ArrayList<>();
        list.forEach(node -> ids.add(node.path("id").asLong()));
        return ids;
    }

    @Test
    @DisplayName("未登录连题目列表都读不到")
    void requiresToken() throws Exception {
        assertThat(apiGet("/api/questions?page=1&size=10", null).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("新建的题能按分页信封查回来，且可编辑")
    void createdQuestionIsListed() throws Exception {
        String token = newUserToken();
        long id = createSingle(token, "IT 建的题 " + newUsername());

        JsonNode res = apiGet("/api/questions?page=1&size=100&scope=mine", token);
        assertThat(res.path("code").asInt()).isZero();
        // PageResult 的形状：list / total / page / size（docs/design/API.md 的通用约定）
        assertThat(res.path("data").path("list").isArray()).isTrue();
        assertThat(res.path("data").path("page").asLong()).isEqualTo(1);
        assertThat(res.path("data").path("size").asLong()).isEqualTo(100);
        assertThat(res.path("data").has("total")).isTrue();
        assertThat(ids(res.path("data").path("list"))).contains(id);
        assertThat(res.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("别人的私有题：连看都看不到，一律 404（连存在性都不确认）")
    void otherUsersPrivateQuestionIsInvisible() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        String stem = "IT 私有题 " + newUsername();
        long id = createSingle(owner, stem);

        // 可见性规则是「公开题 或 自己的题」（docs/design/数据模型.md），所以别人的私有题
        // 对其他人**根本不存在**——给 404 而不是 403，顺带不让人拿 id 探测别人有哪些题。
        assertThat(apiGet("/api/questions/" + id, other).path("code").asInt()).isEqualTo(404);
        assertThat(apiPut("/api/questions/" + id, other, validBody("试图改别人的题")).path("code").asInt())
                .isEqualTo(404);
        assertThat(apiDelete("/api/questions/" + id, other).path("code").asInt()).isEqualTo(404);

        // 而且原文真的没被改动——只断言状态码会被「先报错、后偷偷改」骗过。
        assertThat(apiGet("/api/questions/" + id, owner).path("data").path("stem").asText()).isEqualTo(stem);
    }

    @Test
    @DisplayName("公开题（种子题库）：看得到但改不了，报 403")
    void publicQuestionIsReadOnly() throws Exception {
        String token = newUserToken();

        JsonNode pub = apiGet("/api/questions?page=1&size=5&scope=public", token);
        JsonNode list = pub.path("data").path("list");
        assertThat(list.size()).as("种子题库应当有公开题；没有的话这条用例的前提就不成立了").isPositive();
        // ⚠️ 不能用 path("ownerId").isNull() 判断：接口配了 `non_null`，null 字段直接不出现，
        //    拿到的会是 MissingNode，而 MissingNode.isNull() 是 false。
        assertThat(list.get(0).hasNonNull("ownerId")).as("公开题的 ownerId 应当为空").isFalse();
        assertThat(list.get(0).path("editable").asBoolean()).isFalse();

        // 公开题与「别人的私有题」不同：它**是可见的**，所以「看得到但改不了」用 403 才说得通。
        long publicId = list.get(0).path("id").asLong();
        assertThat(apiPut("/api/questions/" + publicId, token, validBody("试图改公开题")).path("code").asInt())
                .isEqualTo(403);
        assertThat(apiDelete("/api/questions/" + publicId, token).path("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("越权删除：别人的题还在他名下")
    void crossUserDeleteDoesNotRemove() throws Exception {
        String owner = newUserToken();
        String attacker = newUserToken();
        long id = createSingle(owner, "IT 越权删除 " + newUsername());

        assertThat(apiDelete("/api/questions/" + id, attacker).path("code").asInt()).isEqualTo(404);

        JsonNode mine = apiGet("/api/questions?page=1&size=100&scope=mine", owner);
        assertThat(ids(mine.path("data").path("list"))).contains(id);
    }
}
