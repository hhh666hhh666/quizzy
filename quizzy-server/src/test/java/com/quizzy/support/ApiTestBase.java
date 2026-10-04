package com.quizzy.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 接口与集成测试（{@code *IT}）的基类：**真 Spring 上下文 + 真 MySQL**。
 *
 * <p>设计取舍见 docs/testing/系统说明.md 与 docs/adr/0020：
 *
 * <ul>
 *   <li><b>用真 MySQL（Testcontainers）而不是 H2</b>——迁移脚本是 MySQL 方言，
 *       H2 跑不通，用它等于把「测试环境与真实环境不一致」请回来。
 *   <li><b>容器声明为基类的 static 字段</b>——static 字段由所有子类共享，
 *       于是整个测试套件只起一个 MySQL；上下文参数相同，Spring 的上下文缓存也复用得起来。
 *   <li><b>命名以 {@code IT} 结尾</b>——surefire 默认不认这个名字，所以
 *       {@code mvn test} 依旧只跑纯单元测试、不需要 Docker（那条约束见
 *       docs/testing/README.md）；要跑本层得显式 {@code mvn verify}。
 * </ul>
 *
 * <p>⚠️ 用例之间**共享同一个库**，所以每个用例必须用自己造的数据（例如用户名带随机后缀），
 * 不能假设库是干净的。
 */
@Testcontainers
@SpringBootTest(properties = {
        // 测试专用密钥，不是秘密：只为让 token 的签发与校验在测试里可预期。
        // 不写它也能跑——JwtUtil 检测到仓库里那串默认值会改为启动时随机生成——
        // 但那样用例的行为就间接依赖了那段兜底逻辑。
        "quizzy.jwt.secret=quizzy-it-only-secret-0123456789abcdefghijklmn"
})
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
