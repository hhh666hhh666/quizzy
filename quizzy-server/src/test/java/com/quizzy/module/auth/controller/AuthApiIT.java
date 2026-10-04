package com.quizzy.module.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证模块的接口测试：钉住**契约**（统一信封、错误码落在信封里）与**鉴权行为**。
 *
 * <p>这些断言的价值不在于覆盖率，而在于它们是「改接口时会不会悄悄改坏约定」的哨兵——
 * 例如信封结构一旦被改成裸对象返回，这里立刻红。
 */
@DisplayName("接口 · 认证")
class AuthApiIT extends ApiTestBase {

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
}
