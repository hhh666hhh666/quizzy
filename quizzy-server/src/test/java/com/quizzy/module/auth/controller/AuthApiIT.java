package com.quizzy.module.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.quiz.entity.QuizAnswer;
import com.quizzy.module.quiz.entity.QuizSession;
import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.mapper.QuizAnswerMapper;
import com.quizzy.module.quiz.mapper.QuizSessionMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证模块的接口测试：钉住**契约**（统一信封、错误码落在信封里）与**鉴权行为**。
 *
 * <p>这些断言的价值不在于覆盖率，而在于它们是「改接口时会不会悄悄改坏约定」的哨兵——
 * 例如信封结构一旦被改成裸对象返回，这里立刻红。
 */
@DisplayName("接口 · 认证")
class AuthApiIT extends ApiTestBase {

    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionStatMapper questionStatMapper;
    @Autowired
    private QuizSessionMapper quizSessionMapper;
    @Autowired
    private QuizAnswerMapper quizAnswerMapper;

    private static String pngDataUrl() {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_BYTES);
    }

    @Test
    @DisplayName("注册返回统一信封与 token，拿着它能访问 /me 并拿到同一个用户名")
    void registerThenMe() throws Exception {
        String username = newUsername();

        JsonNode res = apiPost("/api/auth/register", null, payload(
                "username", username, "password", "it-Passw0rd", "nickname", "IT 昵称"));

        // 信封：code / message / data / timestamp 一个都不能少（docs/design/API.md 的通用约定）
        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.hasNonNull("message")).isTrue();
        assertThat(res.hasNonNull("timestamp")).isTrue();
        assertThat(res.path("data").path("token").asText()).isNotEmpty();
        assertThat(res.path("data").path("user").path("username").asText()).isEqualTo(username);
        assertThat(res.path("data").path("user").path("nickname").asText()).isEqualTo("IT 昵称");

        // 换个请求带着 token 回来——这一步才真正验了「token 能过拦截器」，
        // 而不只是「注册接口返回了一个字符串」。
        JsonNode me = apiGet("/api/auth/me", res.path("data").path("token").asText());
        assertThat(me.path("code").asInt()).isZero();
        assertThat(me.path("data").path("username").asText()).isEqualTo(username);
    }

    @Test
    @DisplayName("昵称留空时回落成用户名")
    void nicknameFallsBackToUsername() throws Exception {
        String username = newUsername();
        JsonNode res = apiPost("/api/auth/register", null, payload(
                "username", username, "password", "it-Passw0rd", "nickname", ""));

        assertThat(res.path("code").asInt()).isZero();
        assertThat(res.path("data").path("user").path("nickname").asText()).isEqualTo(username);
    }

    @Test
    @DisplayName("用户名重复返回 409，且不会建出第二个账号")
    void duplicateUsernameIsRejected() throws Exception {
        String username = newUsername();
        var body = payload("username", username, "password", "it-Passw0rd", "nickname", "");

        assertThat(apiPost("/api/auth/register", null, body).path("code").asInt()).isZero();

        JsonNode again = apiPost("/api/auth/register", null, body);
        assertThat(again.path("code").asInt()).isEqualTo(409);
        assertThat(again.hasNonNull("data")).isFalse();

        // 第二次注册没成功——再用同一个名字密码登录，仍是第一次那个账号，不算被覆盖。
        JsonNode login = apiPost("/api/auth/login", null,
                payload("username", username, "password", "it-Passw0rd"));
        assertThat(login.path("code").asInt()).isZero();
        assertThat(login.path("data").path("user").path("username").asText()).isEqualTo(username);
    }

    @Test
    @DisplayName("密码错与账号不存在给同一个回复，不让人拿登录接口枚举用户名")
    void wrongPasswordIsRejected() throws Exception {
        String username = newUsername();
        apiPost("/api/auth/register", null, payload("username", username, "password", "it-Passw0rd", "nickname", ""));

        JsonNode wrongPassword = apiPost("/api/auth/login", null,
                payload("username", username, "password", "wrong-pass"));
        JsonNode noSuchUser = apiPost("/api/auth/login", null,
                payload("username", newUsername(), "password", "wrong-pass"));

        assertThat(wrongPassword.path("code").asInt()).isEqualTo(401);
        assertThat(noSuchUser.path("code").asInt()).isEqualTo(401);
        assertThat(wrongPassword.path("message").asText())
                .isEqualTo(noSuchUser.path("message").asText());
    }

    @Test
    @DisplayName("不带 token 访问受保护接口：HTTP 仍是 200，401 落在信封里")
    void missingTokenIsEnvelope401() throws Exception {
        // ⚠️ 这条约定反直觉，所以特意钉住：JwtInterceptor 把 HTTP 状态设成 200、
        //    把失败写进信封（见 JwtInterceptor#writeUnauthorized）。
        //    前端是按 envelope.code 判登录态的，改成 HTTP 401 会连带改坏前端。
        JsonNode res = apiGet("/api/auth/me", null);
        assertThat(res.path("code").asInt()).isEqualTo(401);
        assertThat(res.hasNonNull("data")).isFalse();
    }

    @Test
    @DisplayName("token 是伪造的：同样信封 401，不是 500")
    void forgedTokenIsEnvelope401() throws Exception {
        assertThat(apiGet("/api/auth/me", "not-a-real-token").path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("用户名太短触发校验：信封 400，消息里点出字段名")
    void validationErrorNamesTheField() throws Exception {
        JsonNode res = apiPost("/api/auth/register", null, payload(
                "username", "ab", "password", "it-Passw0rd", "nickname", ""));

        assertThat(res.path("code").asInt()).isEqualTo(400);
        // 全局异常处理器把校验错误拼成「字段名 + 提示」，这样前端能定位到哪一项。
        assertThat(res.path("message").asText()).contains("username");
    }

    // ---------- 账户自助：改昵称 / 改头像 / 改密码 / 退出所有设备 / 注销 ----------

    @Test
    @DisplayName("改昵称与头像：/me 立刻返回新值；头像传空串表示改回默认头像")
    void updateProfileChangesNicknameAndAvatar() throws Exception {
        JsonNode account = newAccount();
        String token = account.path("token").asText();
        String username = account.path("user").path("username").asText();
        String avatar = pngDataUrl();

        JsonNode updated = apiPut("/api/auth/me", token, payload("nickname", "新昵称", "avatar", avatar));
        assertThat(updated.path("code").asInt()).isZero();
        assertThat(updated.path("data").path("nickname").asText()).isEqualTo("新昵称");
        assertThat(updated.path("data").path("avatar").asText()).isEqualTo(avatar);
        // 登录名不受影响——「改登录名」是刻意不做的（见《需求范围》的「用户」一条）
        assertThat(updated.path("data").path("username").asText()).isEqualTo(username);

        assertThat(apiGet("/api/auth/me", token).path("data").path("avatar").asText()).isEqualTo(avatar);

        // 空串 = 改回默认的生成头像，而不是「不改」——non_null 收窄下该字段直接不出现
        JsonNode cleared = apiPut("/api/auth/me", token, payload("nickname", "新昵称", "avatar", ""));
        assertThat(cleared.path("code").asInt()).isZero();
        assertThat(cleared.path("data").hasNonNull("avatar")).isFalse();
        assertThat(apiGet("/api/auth/me", token).path("data").hasNonNull("avatar")).isFalse();
    }

    @Test
    @DisplayName("头像非法一律 400，且不会写进库里")
    void rejectsIllegalAvatar() throws Exception {
        String token = newUserToken();

        String svg = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString("<svg/>".getBytes());
        assertThat(apiPut("/api/auth/me", token, payload("nickname", "n", "avatar", svg))
                .path("code").asInt()).isEqualTo(400);
        assertThat(apiPut("/api/auth/me", token, payload("nickname", "n", "avatar", "https://example.com/a.png"))
                .path("code").asInt()).isEqualTo(400);

        assertThat(apiGet("/api/auth/me", token).path("data").hasNonNull("avatar")).isFalse();
    }

    @Test
    @DisplayName("改密码：本机拿到新 token，旧 token 立刻 401——其他设备就是这么掉线的")
    void changePasswordInvalidatesOldToken() throws Exception {
        JsonNode account = newAccount();
        String token = account.path("token").asText();
        String username = account.path("user").path("username").asText();

        assertThat(apiPut("/api/auth/password", token,
                payload("oldPassword", "wrong-pass", "newPassword", "it-NewPass1")).path("code").asInt())
                .isEqualTo(400);

        JsonNode ok = apiPut("/api/auth/password", token,
                payload("oldPassword", "it-Passw0rd", "newPassword", "it-NewPass1"));
        assertThat(ok.path("code").asInt()).isZero();
        String fresh = ok.path("data").path("token").asText();
        assertThat(fresh).isNotEmpty();

        assertThat(apiGet("/api/auth/me", fresh).path("code").asInt()).isZero();
        assertThat(apiGet("/api/auth/me", token).path("code").asInt()).isEqualTo(401);

        // 密码真的换了，不只是把人踢下线
        assertThat(apiPost("/api/auth/login", null, payload("username", username, "password", "it-NewPass1"))
                .path("code").asInt()).isZero();
        assertThat(apiPost("/api/auth/login", null, payload("username", username, "password", "it-Passw0rd"))
                .path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("退出所有设备：本机 token 也一起失效")
    void logoutAllDevicesKillsCurrentToken() throws Exception {
        String token = newUserToken();

        assertThat(apiPost("/api/auth/logout-all", token, null).path("code").asInt()).isZero();
        assertThat(apiGet("/api/auth/me", token).path("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("注销：密码不对不删任何东西；密码对了则账号与整套数据一起真删，用户名立即可重新注册")
    void deleteAccountRemovesEverythingAndFreesUsername() throws Exception {
        JsonNode account = newAccount();
        String token = account.path("token").asText();
        long userId = account.path("user").path("id").asLong();
        String username = account.path("user").path("username").asText();

        // 造一套属于他的数据。刻意用 Mapper 直接插，而不是走各模块的接口——
        // 这条用例要验的是「注销的清理 SQL 覆盖了哪些表」，不是别的模块能不能建数据。
        Question question = new Question();
        question.setType(QuestionType.SINGLE);
        question.setStem("注销用例例题 " + username);
        question.setAnswer("A");
        question.setScore(1);
        question.setOwnerId(userId);
        questionMapper.insert(question);

        QuestionStat stat = new QuestionStat();
        stat.setUserId(userId);
        stat.setQuestionId(question.getId());
        stat.setAnswerCount(1);
        stat.setCorrectCount(1);
        stat.setConsecutiveCorrect(1);
        stat.setLastCorrect(1);
        stat.setInWrongBook(0);
        questionStatMapper.insert(stat);

        QuizSession session = new QuizSession();
        session.setUserId(userId);
        session.setSourceType(SourceType.QUICK);
        session.setTitle("注销用例会话");
        session.setQuestionCount(1);
        session.setCurrentIndex(1);
        session.setTotalScore(1);
        session.setObtainedScore(1);
        session.setStatus(SessionStatus.COMPLETED);
        quizSessionMapper.insert(session);

        QuizAnswer answer = new QuizAnswer();
        answer.setSessionId(session.getId());
        answer.setQuestionId(question.getId());
        answer.setUserAnswer("A");
        answer.setIsCorrect(1);
        answer.setScore(1);
        answer.setSort(0);
        quizAnswerMapper.insert(answer);

        // 密码不对 → 400，且一个字节都不能动
        assertThat(apiPost("/api/auth/delete-account", token, payload("password", "wrong-pass"))
                .path("code").asInt()).isEqualTo(400);
        assertThat(questionMapper.selectById(question.getId())).as("密码不对时不能动数据").isNotNull();

        assertThat(apiPost("/api/auth/delete-account", token, payload("password", "it-Passw0rd"))
                .path("code").asInt()).isZero();

        // 账号没了：旧 token 立刻作废（拦截器查不到用户），用户名被释放
        assertThat(apiGet("/api/auth/me", token).path("code").asInt()).isEqualTo(401);
        assertThat(apiPost("/api/auth/register", null, payload(
                "username", username, "password", "it-Passw0rd", "nickname", "")).path("code").asInt()).isZero();

        // 他的数据也一起没了
        assertThat(questionMapper.selectById(question.getId())).isNull();
        assertThat(questionStatMapper.selectCount(
                new LambdaQueryWrapper<QuestionStat>().eq(QuestionStat::getUserId, userId))).isZero();
        assertThat(quizSessionMapper.selectCount(
                new LambdaQueryWrapper<QuizSession>().eq(QuizSession::getUserId, userId))).isZero();
        assertThat(quizAnswerMapper.selectCount(
                new LambdaQueryWrapper<QuizAnswer>().eq(QuizAnswer::getSessionId, session.getId()))).isZero();
    }

    @Test
    @DisplayName("账户自助的接口都不能裸奔：不带 token 一律信封 401")
    void accountEndpointsRequireLogin() throws Exception {
        assertThat(apiPut("/api/auth/me", null, payload("nickname", "n", "avatar", ""))
                .path("code").asInt()).isEqualTo(401);
        assertThat(apiPut("/api/auth/password", null,
                payload("oldPassword", "a", "newPassword", "it-NewPass1")).path("code").asInt()).isEqualTo(401);
        assertThat(apiPost("/api/auth/logout-all", null, null).path("code").asInt()).isEqualTo(401);
        assertThat(apiPost("/api/auth/delete-account", null, payload("password", "x"))
                .path("code").asInt()).isEqualTo(401);
    }
}
