package com.quizzy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口与集成测试（{@code *IT}）的基类：**真 Spring 上下文 + 真 MySQL**，外加几个请求助手。
 *
 * <p>设计取舍见 docs/testing/系统说明.md 与 docs/adr/0020：
 *
 * <ul>
 *   <li><b>用真 MySQL（Testcontainers）而不是 H2</b>——迁移脚本是 MySQL 方言，H2 跑不通，
 *       用它等于把「测试环境与真实环境不一致」请回来（ADR 0006 的同类教训）。
 *   <li><b>整棵树只起一个 MySQL</b>，见下面那段注释——这块踩过一次，别再改回去。
 *   <li><b>类名以 {@code IT} 结尾</b>——surefire 默认不认这个名字，所以 {@code mvn test}
 *       依旧只跑纯单元测试、不需要 Docker；要跑本层得显式 {@code mvn verify}。
 * </ul>
 *
 * <p>⚠️ 两条写用例时必须记住的事：
 *
 * <ol>
 *   <li><b>用例之间共享同一个库</b>，谁都不能假设库是干净的——造数据一律带上自己的随机标识。
 *   <li><b>HTTP 状态码几乎总是 200</b>：业务失败与鉴权失败都体现在信封的 {@code code} 里
 *       （{@code JwtInterceptor} 与 {@code GlobalExceptionHandler} 都不改 HTTP 状态）。
 *       所以助手统一按 200 校验，再由用例自己断言 {@code code}。
 * </ol>
 */
@SpringBootTest(properties = {
        // 测试专用密钥，不是秘密：只为让 token 的签发与校验在测试里可预期。
        // 不写它也能跑——JwtUtil 检测到仓库里那串默认值会改为启动时随机生成——
        // 但那样用例的行为就间接依赖了那段兜底逻辑。
        "quizzy.jwt.secret=quizzy-it-only-secret-0123456789abcdefghijklmn"
})
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    /**
     * ⚠️ **单例容器**：手工起一次、整个 JVM 里不再停它（JVM 退出时由 Testcontainers 的 Ryuk 回收）。
     *
     * <p>刻意**不用** {@code @Testcontainers} + {@code @Container} 那套写法。那个扩展会在
     * **每个测试类结束后**停掉 static 容器字段；而这个字段在抽象基类里、对每个子类都生效一次，
     * 于是每跑完一个 IT 类容器就被停掉，Spring 缓存的应用上下文却还指着旧端口——
     * 下一个类直接连不上，报的是 {@code Connection refused}，
     * 日志里能看到**同一个套件里出现好几个不同的随机端口**（2026-10-04 实测踩过，五个）。
     *
     * <p>单例写法让「一个容器」与「一个被缓存的应用上下文」这两件事终于对得上，
     * 顺带把整个套件的耗时压下来。
     */
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    // ---------- 造账号 ----------

    /** 每个用例自己造账号名：库是共享的，不能假设干净。 */
    protected static String newUsername() {
        return "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 注册一个全新账号，返回注册响应里的 data 节点（含 token 与 user.id）。 */
    protected String newUserToken() throws Exception {
        return newAccount().path("token").asText();
    }

    /**
     * 注册一个全新账号，返回注册响应里的 {@code data} 节点。
     *
     * <p>服务集成层要用它：**直接调 service 时需要的是 userId，不是 token**——
     * 而接口层需要的又是 token。一个账号两份凭据，从同一个响应里取。
     */
    protected JsonNode newAccount() throws Exception {
        JsonNode res = apiPost("/api/auth/register", null, payload(
                "username", newUsername(), "password", "it-Passw0rd", "nickname", ""));
        assertThat(res.path("code").asInt()).as("注册应当成功，实际响应：%s", res).isZero();
        return res.path("data");
    }

    protected String register(String username) throws Exception {
        JsonNode res = apiPost("/api/auth/register", null, payload(
                "username", username, "password", "it-Passw0rd", "nickname", ""));
        assertThat(res.path("code").asInt()).as("注册应当成功，实际响应：%s", res).isZero();
        return res.path("data").path("token").asText();
    }

    // ---------- 发请求 ----------

    protected JsonNode apiGet(String path, String token) throws Exception {
        return apiCall(get(path), token, null);
    }

    protected JsonNode apiPost(String path, String token, Object body) throws Exception {
        return apiCall(post(path), token, body);
    }

    protected JsonNode apiPut(String path, String token, Object body) throws Exception {
        return apiCall(put(path), token, body);
    }

    protected JsonNode apiDelete(String path, String token) throws Exception {
        return apiCall(delete(path), token, null);
    }

    /**
     * 发一次请求并把信封解析成 JsonNode。**统一按 HTTP 200 校验**——这个项目的失败
     * 也走 200（见类注释），所以 HTTP 非 200 本身就是「约定被破坏」的信号。
     */
    protected JsonNode apiCall(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mvc.perform(builder).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** 构造请求体：{@code payload("k", v, "k2", v2)}，允许值为 null（{@code Map.of} 不允许）。 */
    protected static Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
