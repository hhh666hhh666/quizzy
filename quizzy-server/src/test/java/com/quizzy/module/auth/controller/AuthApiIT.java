package com.quizzy.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证模块的接口测试：钉住**契约**（统一信封、错误码落在信封里）与**鉴权行为**。
 *
 * <p>这些断言的价值不在于覆盖率，而在于它们是「改接口时会不会悄悄改坏约定」的哨兵——
 * 例如信封结构一旦被改成裸对象返回，这里立刻红。
 */
@DisplayName("接口 · 认证")
class AuthApiIT extends ApiTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    /** 每个用例自己造账号名：库是共享的，不能假设干净。 */
    private static String newUsername() {
        return "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String body(String username, String password, String nickname) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}";
    }

    @Test
    @DisplayName("注册返回统一信封与 token，拿着它能访问 /me 并拿到同一个用户名")
    void registerThenMe() throws Exception {
        String username = newUsername();

        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, "it-Passw0rd", "IT 昵称")))
                .andExpect(status().isOk())
                // 信封：code / message / data / timestamp 一个都不能少（docs/design/API.md 的通用约定）
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(jsonPath("$.data.user.nickname").value("IT 昵称"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("token").asText();

        // 换个请求带着 token 回来 —— 这一步才真正验了「token 能过拦截器」，
        // 而不只是「注册接口返回了一个字符串」。
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username));
    }

    @Test
    @DisplayName("昵称留空时回落成用户名")
    void nicknameFallsBackToUsername() throws Exception {
        String username = newUsername();

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, "it-Passw0rd", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.nickname").value(username));
    }

    @Test
    @DisplayName("用户名重复返回 409，且不会建出第二个账号")
    void duplicateUsernameIsRejected() throws Exception {
        String username = newUsername();
        String json = body(username, "it-Passw0rd", "");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 第二次注册没成功——再用同一个名字密码登录，仍是第一次那个账号，不算被覆盖。
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, "it-Passw0rd", "")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value(username));
    }

    @Test
    @DisplayName("密码错返回 401，且不回显是用户名错还是密码错")
    void wrongPasswordIsRejected() throws Exception {
        String username = newUsername();
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body(username, "it-Passw0rd", "")));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, "wrong-pass", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        // 不存在的账号与密码错给同一个回复——不让人拿登录接口枚举出哪些用户名存在。
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newUsername(), "wrong-pass", "")))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("不带 token 访问受保护接口：HTTP 仍是 200，401 落在信封里")
    void missingTokenIsEnvelope401() throws Exception {
        // ⚠️ 这条约定反直觉，所以特意钉住：JwtInterceptor 把 HTTP 状态设成 200、
        //    把失败写进信封（见 JwtInterceptor#writeUnauthorized）。
        //    前端是按 envelope.code 判登录态的，改成 HTTP 401 会连带改坏前端。
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("token 是伪造的：同样信封 401，不是 500")
    void forgedTokenIsEnvelope401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("用户名太短触发校验：信封 400，消息里点出字段名")
    void validationErrorNamesTheField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ab", "it-Passw0rd", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                // 全局异常处理器把校验错误拼成「字段名 + 提示」，这样前端能定位到哪一项。
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));
    }
}
