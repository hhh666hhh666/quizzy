package com.quizzy.module.paper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 试卷模块的接口测试：契约（固定卷存题目列表、规则卷存条件）+ **数据边界**。
 *
 * <p>⚠️ 试卷的越权用的是 **404 而不是 403**：{@code PaperService#requireOwned} 对「不存在」与
 * 「不属于你」给同一个回复，避免拿 id 探测别人有哪些试卷。这条与题目模块的 403 刻意不同，
 * 所以两边都写成断言——不写下来，早晚有人「统一」掉其中一边。
 */
@DisplayName("接口 · 试卷")
class PaperApiIT extends ApiTestBase {

    private long createSingle(String token) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 试卷用题 " + newUsername(),
                "difficulty", "EASY",
                "score", 3,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("造题失败：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long createFixedPaper(String token, String title, List<Long> questionIds) throws Exception {
        JsonNode res = apiPost("/api/papers", token, payload(
                "title", title, "description", "IT 描述", "mode", "FIXED", "questionIds", questionIds));
        assertThat(res.path("code").asInt()).as("建卷失败：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long papersTotal(String token) throws Exception {
        return apiGet("/api/papers?page=1&size=1", token).path("data").path("total").asLong();
    }

    @Test
    @DisplayName("未登录读不到试卷列表")
    void requiresToken() throws Exception {
        assertThat(apiGet("/api/papers?page=1&size=10", null).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("固定卷：存的是题目列表，详情能原样取回")
    void fixedPaperKeepsQuestionList() throws Exception {
        String token = newUserToken();
        long q1 = createSingle(token);
        long q2 = createSingle(token);
        long paperId = createFixedPaper(token, "IT 固定卷 " + newUsername(), List.of(q1, q2));

        JsonNode detail = apiGet("/api/papers/" + paperId, token);
        assertThat(detail.path("code").asInt()).isZero();
        assertThat(detail.path("data").path("mode").asText()).isEqualTo("FIXED");
        assertThat(detail.path("data").path("questionIds").size()).isEqualTo(2);
        assertThat(detail.path("data").path("questionCount").asInt()).isEqualTo(2);

        // 它也得出现在自己的列表里
        JsonNode page = apiGet("/api/papers?page=1&size=100", token);
        assertThat(page.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);
        boolean found = false;
        for (JsonNode node : page.path("data").path("list")) {
            if (node.path("id").asLong() == paperId) {
                found = true;
            }
        }
        assertThat(found).as("新建的卷应当出现在自己的列表里").isTrue();
    }

    @Test
    @DisplayName("别人的卷：详情 / 改 / 删一律 404，连存在性都不给确认")
    void otherUsersPaperLooksMissing() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        long paperId = createFixedPaper(owner, "IT 别人的卷 " + newUsername(), List.of(createSingle(owner)));

        assertThat(apiGet("/api/papers/" + paperId, other).path("code").asInt()).isEqualTo(404);
        assertThat(apiPut("/api/papers/" + paperId, other, payload(
                "title", "试图改别人的卷", "mode", "FIXED", "questionIds", List.of()))
                .path("code").asInt()).isEqualTo(404);
        assertThat(apiDelete("/api/papers/" + paperId, other).path("code").asInt()).isEqualTo(404);

        // 真的没被改也没被删
        JsonNode mine = apiGet("/api/papers/" + paperId, owner);
        assertThat(mine.path("code").asInt()).isZero();
        assertThat(mine.path("data").path("questionCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("规则卷预览只出题、不落库")
    void previewDoesNotCreatePaper() throws Exception {
        String token = newUserToken();
        long before = papersTotal(token);

        JsonNode res = apiPost("/api/papers/preview", token, payload(
                "types", List.of("SINGLE"), "difficulties", List.of()));

        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").isArray()).isTrue();
        assertThat(res.path("data").size()).as("种子题库里有单选题，预览不该是空的").isPositive();
        assertThat(papersTotal(token)).as("预览不该建出试卷").isEqualTo(before);
    }

    // ---------- 空卷与追加题目（ADR 0026） ----------

    @Test
    @DisplayName("空固定卷能建：题留到之后再往里加")
    void emptyFixedPaperIsAllowed() throws Exception {
        String token = newUserToken();
        long paperId = createFixedPaper(token, "IT 空卷 " + newUsername(), List.of());

        JsonNode detail = apiGet("/api/papers/" + paperId, token);
        assertThat(detail.path("code").asInt()).isZero();
        assertThat(detail.path("data").path("questionCount").asInt()).isZero();
        assertThat(detail.path("data").path("questionIds").size()).isZero();
    }

    @Test
    @DisplayName("追加题目：并入已有列表、重复的忽略，回执给新增数与总数")
    void appendQuestionsMergesAndIgnoresDuplicates() throws Exception {
        String token = newUserToken();
        long q1 = createSingle(token);
        long q2 = createSingle(token);
        long paperId = createFixedPaper(token, "IT 追加卷 " + newUsername(), List.of(q1));

        // 把已在卷里的 q1 与新题 q2 一起提交：q1 该被忽略
        JsonNode res = apiPost("/api/papers/" + paperId + "/questions", token,
                payload("questionIds", List.of(q1, q2)));

        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("added").asInt()).isEqualTo(1);
        assertThat(res.path("data").path("total").asInt()).isEqualTo(2);
        assertThat(apiGet("/api/papers/" + paperId, token).path("data").path("questionCount").asInt())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("追加题目：空入参被参数校验拦下（400），不是静默成功")
    void appendWithEmptyIdsIsRejected() throws Exception {
        String token = newUserToken();
        long paperId = createFixedPaper(token, "IT 空参追加卷 " + newUsername(), List.of());

        assertThat(apiPost("/api/papers/" + paperId + "/questions", token, payload("questionIds", List.of()))
                .path("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("追加题目到别人的卷：404，且真的没加进去")
    void appendToOtherUsersPaperLooksMissing() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        long paperId = createFixedPaper(owner, "IT 别人的追加卷 " + newUsername(), List.of());
        long myQuestion = createSingle(other);

        assertThat(apiPost("/api/papers/" + paperId + "/questions", other,
                payload("questionIds", List.of(myQuestion))).path("code").asInt()).isEqualTo(404);

        // 只断言状态码会被「先报错、后偷偷改」骗过，所以回查一次
        assertThat(apiGet("/api/papers/" + paperId, owner).path("data").path("questionCount").asInt()).isZero();
    }

    @Test
    @DisplayName("存卷时塞别人的私有题：404——这是可见性边界，不是 400 参数错")
    void fixedPaperRejectsOthersPrivateQuestion() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        long privateId = createSingle(owner);

        assertThat(apiPost("/api/papers", other, payload(
                "title", "IT 越权存卷 " + newUsername(), "mode", "FIXED", "questionIds", List.of(privateId)))
                .path("code").asInt()).isEqualTo(404);
    }
}
