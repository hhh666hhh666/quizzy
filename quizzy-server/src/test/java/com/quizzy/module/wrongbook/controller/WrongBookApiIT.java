package com.quizzy.module.wrongbook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错题本模块的接口测试。
 *
 * <p>错题本**不是一张独立的表**，而是从作答统计投影出来的状态（见 docs/design/数据模型.md）。
 * 所以这一层的断言实际上是「投影对不对」——答错一次就该出现，移出就该消失。
 */
@DisplayName("接口 · 错题本")
class WrongBookApiIT extends ApiTestBase {

    private JsonNode wrongBook(String token) throws Exception {
        return apiGet("/api/wrong-book?page=1&size=100", token);
    }

    private static List<Long> ids(JsonNode pageEnvelope) {
        List<Long> ids = new ArrayList<>();
        pageEnvelope.path("data").path("list").forEach(node -> ids.add(node.path("id").asLong()));
        return ids;
    }

    /** 快速练习必须带抽题规则（规则为空会被直接拒掉）；规则里的题量是上限，见 QuizApiIT 里的说明。 */
    private long startQuick(String token) throws Exception {
        JsonNode res = apiPost("/api/quiz/start", token, payload(
                "sourceType", "QUICK", "rule", payload("types", List.of("SINGLE"))));
        assertThat(res.path("code").asInt()).as("发起快速练习失败，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    private long practice(String token, int count) throws Exception {
        JsonNode res = apiPost("/api/wrong-book/practice?count=" + count, token, null);
        assertThat(res.path("code").asInt()).as("错题重练应当能发起，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    private JsonNode firstQuestion(String token, long sessionId) throws Exception {
        return apiGet("/api/quiz/sessions/" + sessionId, token).path("data").path("questions").get(0);
    }

    /** 把分发的正确答案拼成一次提交的形式——多选少选同样不得分（ADR 0004）。 */
    private static String joinAnswers(JsonNode correctAnswers) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : correctAnswers) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(node.asText());
        }
        return sb.toString();
    }

    /** 发起一次快速练习，答错第一题，返回该题的 id。 */
    private long answerOneWrong(String token) throws Exception {
        long sessionId = startQuick(token);
        long questionId = firstQuestion(token, sessionId).path("questionId").asLong();

        JsonNode res = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", "Z"));
        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("isCorrect").asBoolean()).as("提交一个不存在的选项必须判错").isFalse();
        return questionId;
    }

    @Test
    @DisplayName("未登录读不到错题本")
    void requiresToken() throws Exception {
        assertThat(apiGet("/api/wrong-book?page=1&size=10", null).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("全新账号的错题本是空的")
    void freshAccountHasEmptyWrongBook() throws Exception {
        JsonNode res = wrongBook(newUserToken());
        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("total").asLong()).isZero();
    }

    @Test
    @DisplayName("答错就进错题本；手动移出后就消失")
    void wrongAnswerEntersAndManualRemoveClears() throws Exception {
        String token = newUserToken();
        long questionId = answerOneWrong(token);

        assertThat(ids(wrongBook(token))).as("答错应当进错题本").contains(questionId);

        JsonNode removed = apiDelete("/api/wrong-book/" + questionId, token);
        assertThat(removed.path("code").asInt()).isZero();

        assertThat(ids(wrongBook(token))).as("手动移出后不该还在").doesNotContain(questionId);
    }

    @Test
    @DisplayName("错题重练抽到的就是错题本里的题")
    void practiceDrawsFromWrongBook() throws Exception {
        String token = newUserToken();
        long questionId = answerOneWrong(token);

        long sessionId = practice(token, 1);
        assertThat(firstQuestion(token, sessionId).path("questionId").asLong())
                .as("错题重练只该出错题本里的题")
                .isEqualTo(questionId);
    }

    @Test
    @DisplayName("连续答对若干次后自动移出错题本（次数以代码为准，不在这里写死）")
    void repeatedCorrectAnswersGraduallyRemoveIt() throws Exception {
        String token = newUserToken();

        // 先答错一次：既让它进错题本，也顺势拿到正确答案（未作答时接口不会下发答案）。
        long sessionId = startQuick(token);
        long questionId = firstQuestion(token, sessionId).path("questionId").asLong();
        JsonNode wrong = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", "Z"));
        String correct = joinAnswers(wrong.path("data").path("correctAnswers"));
        assertThat(correct).isNotEmpty();
        assertThat(ids(wrongBook(token))).contains(questionId);

        // ⚠️ 掌握阈值「具体数值以代码为准」（docs/design/判分与业务规则.md 明说），
        //    所以这里**不写死次数**，只设一个上限防死循环：撞到上限仍是红，说明行为变了。
        int bound = 12;
        int rounds = 0;
        while (rounds < bound && ids(wrongBook(token)).contains(questionId)) {
            long round = practice(token, 1);
            apiPost("/api/quiz/sessions/" + round + "/answer", token,
                    payload("questionId", questionId, "answer", correct));
            rounds++;
        }

        assertThat(ids(wrongBook(token)))
                .as("连续答对 %d 轮后仍没移出错题本——掌握规则变了，或者判分没落库", bound)
                .doesNotContain(questionId);
        assertThat(rounds).as("不该在第一轮就移出（那说明答错根本没进过本）").isGreaterThan(0);
    }
}
