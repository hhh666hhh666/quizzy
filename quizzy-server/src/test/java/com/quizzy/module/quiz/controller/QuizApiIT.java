package com.quizzy.module.quiz.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 答题会话模块的接口测试：判分落库、结算自洽、**会话归属**。
 *
 * <p>沿用项目「可回退改答案」的语义：同一题重复提交以最后一次为准——这条也写成断言，
 * 因为它是「练习语义」而不是「考试语义」的直接体现（ADR 0001）。
 */
@DisplayName("接口 · 答题会话")
class QuizApiIT extends ApiTestBase {

    /**
     * 发起一次快速练习（不带题量，走默认值）。
     *
     * <p>⚠️ 快速练习**必须带抽题规则**——{@code QuizService#start} 的 QUICK 分支里规则为空会直接报
     * 「请配置抽题规则」。
     *
     * <p>题量在**规则里**（{@code PaperRuleDTO.count}，默认 20、夹在 1–200），语义是**上限**：
     * {@code PaperService#selectQuestionsByRule} 会 {@code ORDER BY RAND() LIMIT count}，
     * 匹配不足时有多少给多少。所以**不传 count 的用例不能写死题数**——拿到的是匹配数。
     *
     * <p>⚠️ 别把它和 {@code QuizStartDTO.count} 混了：**那个只对错题重练（WRONG_BOOK）生效**，
     * 快速练习根本不看它。两个同名字段是两回事。
     */
    private long startQuick(String token) throws Exception {
        JsonNode res = apiPost("/api/quiz/start", token, payload(
                "sourceType", "QUICK",
                "rule", payload("types", List.of("SINGLE"))));
        assertThat(res.path("code").asInt()).as("发起快速练习失败，实际响应：%s", res).isZero();
        return res.path("data").asLong();
    }

    private JsonNode session(String token, long sessionId) throws Exception {
        return apiGet("/api/quiz/sessions/" + sessionId, token);
    }

    private static JsonNode firstQuestion(JsonNode sessionEnvelope) {
        return sessionEnvelope.path("data").path("questions").get(0);
    }

    /**
     * 把分发的正确答案拼成「一次提交」的形式。
     *
     * <p>⚠️ 必须拼全：抽到的题可能是多选，而多选**少选同样不得分**（ADR 0004）。
     * 只提交第一个正确答案的写法在单选的题上能过、在多选的题上会莫名其妙地红。
     * 存储格式是逗号分隔的 label（见 {@code AnswerUtil}）。
     */
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

    @Test
    @DisplayName("未登录发不起会话")
    void requiresToken() throws Exception {
        assertThat(apiPost("/api/quiz/start", null, payload("sourceType", "QUICK", "count", 3))
                .path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("快速练习：详情带出题目，且一开始都未作答")
    void quickSessionIsUnanswered() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);

        JsonNode res = session(token, sessionId);
        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(res.path("data").path("sourceType").asText()).isEqualTo("QUICK");
        JsonNode questions = res.path("data").path("questions");
        // 这条没传题量 → 后端用默认 20 作上限；种子题库里的单选题不足 20 道，所以拿到的是匹配数。
        // 因此只断言「两个数对得上」，不写死个数。想验题量本身请看 ruleCountIsAnUpperBound。
        assertThat(questions.size()).isEqualTo(res.path("data").path("questionCount").asInt());
        assertThat(questions.size()).isPositive();
        assertThat(questions.get(0).path("answered").asBoolean()).isFalse();
        // 题目与分值都该带出来，前端才能渲染与算总分
        assertThat(questions.get(0).path("questionId").asLong()).isPositive();
        assertThat(res.path("data").path("totalScore").asInt()).isPositive();
    }

    @Test
    @DisplayName("规则里的题量是上限：传 3 就只抽 3 道，不是把匹配到的全给")
    void ruleCountIsAnUpperBound() throws Exception {
        String token = newUserToken();

        // 先问出「匹配到多少道」——用一个足够大的题量拿全量，这样断言不依赖种子题库的具体条数。
        JsonNode matched = apiPost("/api/papers/preview", token,
                payload("types", List.of("SINGLE"), "count", 200));
        assertThat(matched.path("code").asInt()).isZero();
        // ⚠️ 这里必须是**严格大于** 3：匹配数要是正好 3，下面那条「传 3 抽到 3」就区分不出
        //    「题量生效」和「把匹配到的全给」——那样这条用例等于没验。
        assertThat(matched.path("data").size())
                .as("种子题库里的单选题必须多于 3 道，这条用例才有鉴别力")
                .isGreaterThan(3);

        JsonNode res = apiPost("/api/quiz/start", token, payload(
                "sourceType", "QUICK",
                "rule", payload("types", List.of("SINGLE"), "count", 3)));
        assertThat(res.path("code").asInt()).isZero();

        JsonNode opened = session(token, res.path("data").asLong()).path("data");
        // 这一条才是「题量生效」的证据。⚠️ 它同时是 `LIMIT count` 的哨兵：
        // 谁把 `selectQuestionsByRule` 里的 `LIMIT count` 拿掉，这里立刻红——
        // 在此之前，34 个接口用例**没有一个**碰得到这个语义（其余用例都没传 count）。
        assertThat(opened.path("questionCount").asInt())
                .as("题量是上限，应当正好 3 道")
                .isEqualTo(3);
        assertThat(opened.path("questions").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("题量被夹在 1–200：传 0 也至少抽 1 道")
    void ruleCountIsClamped() throws Exception {
        String token = newUserToken();

        JsonNode res = apiPost("/api/quiz/start", token, payload(
                "sourceType", "QUICK",
                "rule", payload("types", List.of("SINGLE"), "count", 0)));
        assertThat(res.path("code").asInt()).isZero();

        // PaperService 里是 `Math.min(Math.max(count, 1), 200)`——下限 1，不是「抽 0 道」。
        assertThat(session(token, res.path("data").asLong()).path("data").path("questionCount").asInt())
                .as("题量下限是 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("提交错答得 0 分并给出正确答案与解析；改答成正确后拿到该题分值")
    void submitThenCorrect() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);
        JsonNode question = firstQuestion(session(token, sessionId));
        long questionId = question.path("questionId").asLong();
        int score = question.path("score").asInt();

        // 故意提交一个不在选项里的作答——不依赖「正确答案是什么」就能保证判错。
        JsonNode wrong = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", "Z"));
        assertThat(wrong.path("code").asInt()).isZero();
        assertThat(wrong.path("data").path("isCorrect").asBoolean()).isFalse();
        assertThat(wrong.path("data").path("obtainedScore").asInt()).isZero();
        // 判错也要把正确答案与解析给出来——即时反馈是练习语义的核心
        assertThat(wrong.path("data").path("correctAnswers").isArray()).isTrue();
        assertThat(wrong.path("data").path("correctAnswers").size()).isPositive();
        assertThat(wrong.hasNonNull("data")).isTrue();

        // 落库了：重新拉详情，这题变成已作答
        JsonNode afterWrong = session(token, sessionId);
        assertThat(afterWrong.path("data").path("questions").get(0).path("answered").asBoolean()).isTrue();
        assertThat(afterWrong.path("data").path("obtainedScore").asInt()).isZero();

        // 可回退改答案：同一题再提交正确答案，以最后一次为准
        String correct = joinAnswers(wrong.path("data").path("correctAnswers"));
        JsonNode fixed = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", correct));
        assertThat(fixed.path("code").asInt()).isZero();
        assertThat(fixed.path("data").path("isCorrect").asBoolean()).isTrue();
        assertThat(fixed.path("data").path("obtainedScore").asInt()).isEqualTo(score);

        assertThat(session(token, sessionId).path("data").path("obtainedScore").asInt()).isEqualTo(score);
    }

    @Test
    @DisplayName("提交一道不属于本会话的题：业务错误，不是 500")
    void answeringForeignQuestionIsRejected() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);

        JsonNode res = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", 999999999L, "answer", "A"));
        assertThat(res.path("code").asInt()).isNotZero();
        assertThat(res.path("code").asInt()).isNotEqualTo(500);
    }

    @Test
    @DisplayName("别人的会话：详情 / 提交 / 结算一律 404，不给确认存在性")
    void otherUsersSessionLooksMissing() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        long sessionId = startQuick(owner);
        long questionId = firstQuestion(session(owner, sessionId)).path("questionId").asLong();

        assertThat(session(other, sessionId).path("code").asInt()).isEqualTo(404);
        assertThat(apiPost("/api/quiz/sessions/" + sessionId + "/answer", other,
                payload("questionId", questionId, "answer", "A")).path("code").asInt()).isEqualTo(404);
        assertThat(apiPost("/api/quiz/sessions/" + sessionId + "/finish", other, null)
                .path("code").asInt()).isEqualTo(404);
        assertThat(session(owner, sessionId).path("data").path("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("结算：已答 + 未答 = 总题数，正确率按已答算，且结算后不能再提交")
    void finishIsConsistent() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);
        JsonNode opened = session(token, sessionId);
        int totalQuestions = opened.path("data").path("questionCount").asInt();
        long questionId = firstQuestion(opened).path("questionId").asLong();

        JsonNode wrong = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", "Z"));
        String correct = joinAnswers(wrong.path("data").path("correctAnswers"));
        apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", correct));

        JsonNode res = apiPost("/api/quiz/sessions/" + sessionId + "/finish", token, null);
        assertThat(res.path("code").asInt()).isZero();
        JsonNode data = res.path("data");
        assertThat(data.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("questionCount").asInt()).isEqualTo(totalQuestions);
        assertThat(data.path("answeredCount").asInt()).isEqualTo(1);
        assertThat(data.path("unansweredCount").asInt()).isEqualTo(totalQuestions - 1);
        assertThat(data.path("answeredCount").asInt() + data.path("unansweredCount").asInt())
                .as("已答 + 未答必须等于总题数")
                .isEqualTo(data.path("questionCount").asInt());
        assertThat(data.path("correctCount").asInt()).isEqualTo(1);
        // ── 正确率这条两处都容易错，所以两处都写下来 ──
        // ① 分母是「已作答」而不是总题数（《判分与业务规则》里写着）；
        // ② **单位是百分数（0–100），不是 0–1 的比例**——写成比例会得到 100.0 而不是 1.0。
        // 按公式断言，不写死数字：条件变了这条断言仍然说得通。
        double expectedAccuracy = data.path("correctCount").asInt() * 100.0
                / data.path("answeredCount").asInt();
        assertThat(data.path("accuracy").asDouble()).isCloseTo(expectedAccuracy, within(1e-9));

        // 已经结算的会话不能再提交
        JsonNode late = apiPost("/api/quiz/sessions/" + sessionId + "/answer", token,
                payload("questionId", questionId, "answer", correct));
        assertThat(late.path("code").asInt()).isNotZero();
    }

    @Test
    @DisplayName("未作答的题不下发正确答案与解析——详情接口不是一份答案")
    void unansweredQuestionsDoNotLeakAnswers() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);

        JsonNode question = firstQuestion(session(token, sessionId));
        assertThat(question.path("answered").asBoolean()).isFalse();
        // 只在已作答时才 set 这几个字段（见 QuizService#detail）。这条不写下来，
        // 早晚有人为了「少一次请求」把它们挪出那个 if —— 那就等于把答案随详情一起发了。
        assertThat(question.hasNonNull("correctAnswers")).as("未作答不该带正确答案").isFalse();
        assertThat(question.hasNonNull("analysis")).as("未作答不该带解析").isFalse();
    }

    @Test
    @DisplayName("放弃会话：状态进终态，不能再提交")
    void abandonEndsSession() throws Exception {
        String token = newUserToken();
        long sessionId = startQuick(token);

        assertThat(apiPost("/api/quiz/sessions/" + sessionId + "/abandon", token, null).path("code").asInt())
                .isZero();

        JsonNode after = session(token, sessionId);
        assertThat(after.path("data").path("status").asText()).isNotEqualTo("IN_PROGRESS");
    }
}
