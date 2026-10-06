package com.quizzy.module.question.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        return createSingle(token, stem, null);
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

    // ---------- 分类筛选（多选 + 未分类） ----------
    // 这一组的存在理由：分类筛选是**多选 + 一个独立开关**拼出来的，而拼 OR 条件时
    // 少一层括号会把「未分类」变成绕过全部其它条件的后门。那种错误不报错、只是多返回数据，
    // 只有断言了「不该出现的必须不出现」才拦得住。

    @Test
    @DisplayName("分类多选：勾哪几个分类，就返回这几个分类里的题（或的关系）")
    void filtersByMultipleCategories() throws Exception {
        String token = newUserToken();
        String stamp = newUsername();
        String nameA = "ITcateA" + stamp;
        String nameB = "ITcateB" + stamp;
        long inA = createSingle(token, "IT分类多选甲" + stamp, nameA);
        long inB = createSingle(token, "IT分类多选乙" + stamp, nameB);
        long idA = categoryIdOfName(token, nameA);
        long idB = categoryIdOfName(token, nameB);

        assertThat(mineIds(token, "categoryIds=" + idA)).as("只勾甲").containsExactly(inA);
        assertThat(mineIds(token, "categoryIds=" + idB)).as("只勾乙").containsExactly(inB);
        assertThat(mineIds(token, "categoryIds=" + idA + "&categoryIds=" + idB))
                .as("两个都勾")
                .containsExactlyInAnyOrder(inA, inB);

        // 一个都不勾 = 不按分类筛（界面上的「全部分类」），该能看到刚才那两道
        assertThat(mineIds(token, "")).contains(inA, inB);
    }

    @Test
    @DisplayName("「未分类」是一个独立开关：只返回没有分类的题")
    void filtersByUncategorized() throws Exception {
        String token = newUserToken();
        String stamp = newUsername();
        long noCategory = createSingle(token, "IT没有分类" + stamp, null);
        long hasCategory = createSingle(token, "IT有分类" + stamp, "ITcateC" + stamp);

        List<Long> got = mineIds(token, "uncategorized=true");
        assertThat(got).as("没有分类的那道应当出现").contains(noCategory);
        assertThat(got).as("有分类的那道不该出现").doesNotContain(hasCategory);
    }

    @Test
    @DisplayName("「未分类」与具体分类同时勾：两类都返回（或的关系）")
    void uncategorizedCombinesWithCategories() throws Exception {
        String token = newUserToken();
        String stamp = newUsername();
        String nameD = "ITcateD" + stamp;
        long inD = createSingle(token, "IT组合分类" + stamp, nameD);
        long inE = createSingle(token, "IT组合另分类" + stamp, "ITcateE" + stamp);
        long noCategory = createSingle(token, "IT组合无分类" + stamp, null);

        List<Long> got = mineIds(token, "categoryIds=" + categoryIdOfName(token, nameD) + "&uncategorized=true");
        assertThat(got).containsExactlyInAnyOrder(inD, noCategory);
        assertThat(got).as("没勾的另一个分类不该被顺带捞出来").doesNotContain(inE);
    }

    @Test
    @DisplayName("「未分类」不会绕过其它筛选条件（拼 OR 时那个括号）")
    void uncategorizedDoesNotEscapeOtherFilters() throws Exception {
        String token = newUserToken();
        String other = newUserToken();
        String stamp = newUsername();
        String nameH = "ITcateH" + stamp;
        // 关键词里不带空格：它要出现在 URL 的查询串里，带空格就得额外编码，没必要给自己找麻烦
        String keyword = "ITGuard" + stamp;
        long mineInCategory = createSingle(token, keyword + "mineC", nameH);
        long mineUncategorized = createSingle(token, keyword + "mineN", null);
        long theirsInCategory = createSingle(other, keyword + "theirsC", nameH);
        long theirsUncategorized = createSingle(other, keyword + "theirsN", null);

        // ⚠️ 这一条必须同时带 categoryIds 与 uncategorized：只有两者都在时才会走
        // 「(category_id IN (...) OR category_id IS NULL)」那条分支。少了 categoryIds，
        // 走的是单条件的 `category_id IS NULL`，压根构造不出那个括号，也就守不住它。
        //
        // 若那层括号丢了，SQL 会退化成 `owner_id = ? AND stem LIKE ? AND category_id IN (?)
        // OR category_id IS NULL`——最后一个 OR 会把「范围=我的」与关键词一起绕过，
        // 于是**别人那道没有分类的题**也会冒出来。
        List<Long> got = mineIds(token, "keyword=" + keyword
                + "&categoryIds=" + categoryIdOfName(token, nameH)
                + "&uncategorized=true");

        assertThat(got).containsExactlyInAnyOrder(mineInCategory, mineUncategorized);
        assertThat(got).as("别人那道未分类的题绝不能因为 OR 逃逸而被捞出来").doesNotContain(theirsUncategorized);
        assertThat(got).as("别人那道有分类的题同样不该出现").doesNotContain(theirsInCategory);
    }

    // ---------- 导出 ----------

    @Test
    @DisplayName("导出跟随筛选条件，且不受分页影响")
    void exportFollowsFilters() throws Exception {
        String token = newUserToken();
        String stamp = newUsername();
        String keep = "IT导出留下" + stamp;
        String drop = "IT导出丢掉" + stamp;
        createSingle(token, keep, "ITcateF" + stamp);
        createSingle(token, drop, "ITcateG" + stamp);

        // 用分页把 size 压到 1：如果导出跟着分页走，就只会导出 1 条，下面「留下的那条在不在」就成了随机
        String body = exportJson(token, "page=1&size=1&categoryIds=" + categoryIdOfName(token, "ITcateF" + stamp));
        assertThat(body).as("筛出来的那条应当在导出内容里").contains(keep);
        assertThat(body).as("没勾的分类不该被导出").doesNotContain(drop);
    }

    @Test
    @DisplayName("导出不给筛选条件时，也拿不到别人的私有题")
    void exportNeverLeaksOthersPrivateQuestions() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        String privateStem = "IT导出别人的私有题" + newUsername();
        createSingle(owner, privateStem, null);

        // 不带任何筛选条件导出 = 「公开题 + 自己的题」；别人的私有题必须一条都不出现
        assertThat(exportJson(other, "")).doesNotContain(privateStem);
    }

    // ---------- 导入时顺带建卷（ADR 0026） ----------
    // 这一组盯的是三件事：卷里装的**只有成功的题**、没给卷名**绝不建卷**、
    // 以及 paperId 在没建卷时**整个字段不出现**（接口配了 non_null，容易断错）。

    @Test
    @DisplayName("JSON 导入给了卷名：本次成功的题装进一张新固定卷，paperId 回给前端")
    void importJsonCanCreatePaper() throws Exception {
        String token = newUserToken();
        String title = "ITimport" + newUsername();

        JsonNode res = apiPost("/api/questions/import/json?paperTitle=" + title, token,
                List.of(importItem("IT导入甲" + newUsername()), importItem("IT导入乙" + newUsername())));

        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("successCount").asInt()).isEqualTo(2);

        long paperId = res.path("data").path("paperId").asLong();
        assertThat(paperId).as("应当建出卷来并把 id 回给前端").isPositive();

        JsonNode detail = apiGet("/api/papers/" + paperId, token);
        assertThat(detail.path("data").path("title").asText()).isEqualTo(title);
        assertThat(detail.path("data").path("mode").asText()).isEqualTo("FIXED");
        assertThat(detail.path("data").path("questionCount").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("JSON 导入没给卷名：不建卷，响应里连 paperId 字段都没有")
    void importJsonWithoutTitleDoesNotCreatePaper() throws Exception {
        String token = newUserToken();
        long before = papersTotal(token);

        JsonNode res = apiPost("/api/questions/import/json", token,
                List.of(importItem("IT导入不建卷" + newUsername())));

        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("successCount").asInt()).isEqualTo(1);
        // ⚠️ 不能写 path("paperId").isNull()：null 字段直接不出现，拿到的是 MissingNode，isNull() 是 false
        assertThat(res.path("data").hasNonNull("paperId")).as("没建卷就不该有 paperId").isFalse();
        assertThat(papersTotal(token)).as("不该凭空多出一张卷").isEqualTo(before);
    }

    // ---------- 助手 ----------

    /** 造一道带分类的单选题；{@code categoryName} 为空即「未分类」。 */
    private long createSingle(String token, String stem, String categoryName) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", stem,
                "difficulty", "EASY",
                "score", 1,
                "answers", List.of("A"),
                "categoryName", categoryName,
                "options", List.of(
                        payload("label", "A", "content", "选项 A"),
                        payload("label", "B", "content", "选项 B"))));
        assertThat(res.path("code").asInt()).as("建题应当成功，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    /** 按「我的题库 + 额外条件」列表，返回命中的题目 id。 */
    private List<Long> mineIds(String token, String extraQuery) throws Exception {
        String path = "/api/questions?page=1&size=100&scope=mine"
                + (extraQuery.isEmpty() ? "" : "&" + extraQuery);
        JsonNode res = apiGet(path, token);
        assertThat(res.path("code").asInt()).as("列表应当成功，实际响应：%s", res).isZero();
        return ids(res.path("data").path("list"));
    }

    /** 分类列表里按名字取 id——分类没有「按名字查」的接口，只能拉全表找。 */
    private long categoryIdOfName(String token, String name) throws Exception {
        for (JsonNode category : apiGet("/api/categories", token).path("data")) {
            if (name.equals(category.path("name").asText())) {
                return category.path("id").asLong();
            }
        }
        throw new AssertionError("分类没找到：" + name);
    }

    /**
     * 发一次 JSON 导出，返回响应体原文。
     *
     * <p>⚠️ 导出**不走统一信封**：响应体就是文件内容本身（一个题目对象的裸数组），
     * 所以不能拿 {@code apiGet} 去解析——那样会把 JSON 数组当成信封读，字段全取不到。
     */
    private String exportJson(String token, String query) throws Exception {
        String path = "/api/questions/export?format=json" + (query.isEmpty() ? "" : "&" + query);
        return mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** 一条能通过校验的 JSON 导入项（裸数组里的一个元素）。 */
    private static Object importItem(String stem) {
        return payload(
                "type", "single",
                "stem", stem,
                "answer", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "选项 A"),
                        payload("label", "B", "content", "选项 B")));
    }

    private long papersTotal(String token) throws Exception {
        return apiGet("/api/papers?page=1&size=1", token).path("data").path("total").asLong();
    }
}
