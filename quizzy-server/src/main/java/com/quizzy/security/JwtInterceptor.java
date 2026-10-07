package com.quizzy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzy.common.Result;
import com.quizzy.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenVersionSource tokenVersionSource;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER)) {
            writeUnauthorized(response);
            return false;
        }
        JwtUtil.TokenPayload payload = jwtUtil.parseToken(header.substring(BEARER.length()));
        if (payload == null || !isStillCurrent(payload)) {
            writeUnauthorized(response);
            return false;
        }
        UserContext.setUserId(payload.userId());
        return true;
    }

    /**
     * 比对 token 版本号：改密码 / 退出所有设备会把 `user.token_version` +1，所有旧 token
     * 下一次请求即作废；查不到用户（已注销）同样作废。
     *
     * <p>⚠️ 这是「无状态 JWT」变成「半状态」的那一处——**每个带 token 的请求都多一次查库**。
     * 代价是明知故犯，理由与替代方案见 docs/adr/0027。
     */
    private boolean isStillCurrent(JwtUtil.TokenPayload payload) {
        Integer current = tokenVersionSource.currentVersion(payload.userId());
        return current != null && current == payload.tokenVersion();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(ResultCode.UNAUTHORIZED));
    }
}
