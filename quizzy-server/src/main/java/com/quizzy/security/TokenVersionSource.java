package com.quizzy.security;

/**
 * 当前用户的 token 版本号从哪查——定义在 security 包里，由业务模块实现。
 *
 * <p>这样切是为了依赖方向：`JwtInterceptor` 只依赖这个接口，不必反向 import `module.auth`
 * （现有方向一直是 module → security，见 `AuthService` 用 `JwtUtil`、`WebConfig` 用 `JwtInterceptor`）。
 *
 * <p>为什么需要它、代价是什么：见 docs/adr/0027。
 */
public interface TokenVersionSource {

    /**
     * 当前用户的 token 版本号；**用户不存在（已被注销）时返回 null**，调用方按无效处理。
     */
    Integer currentVersion(Long userId);
}
