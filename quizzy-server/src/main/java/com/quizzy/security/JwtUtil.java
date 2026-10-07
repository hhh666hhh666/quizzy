package com.quizzy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    /**
     * application.yml 里的兜底默认值。它已随公开仓库暴露，等于一把全网皆知的钥匙，
     * 谁都能拿它签发任意 userId 的 token，绝不能真正用于签名。
     */
    private static final String INSECURE_DEFAULT_SECRET =
            "quizzy-default-jwt-secret-please-replace-in-production-0123456789abcdef";

    /** 承载 token 版本号的 claim 名。短名字只是为了少占 token 体积。 */
    private static final String CLAIM_TOKEN_VERSION = "tv";

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${quizzy.jwt.secret}") String secret,
                   @Value("${quizzy.jwt.expire-days}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(resolveSecret(secret).getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireDays * 24 * 60 * 60 * 1000L;
    }

    /**
     * 没配 QUIZZY_JWT_SECRET 时不能直接用那个公开的默认值签名，
     * 改为每次启动随机生成一把：安全性兜住了，代价只是重启后需要重新登录。
     * （仅影响本机开发；容器里由 .env 注入真实密钥，不会走到这个分支。）
     */
    private String resolveSecret(String secret) {
        if (!INSECURE_DEFAULT_SECRET.equals(secret)) {
            return secret;
        }
        String random = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID();
        log.warn("\n" +
                "****************************************************************************\n" +
                "* 正在使用 JWT 默认密钥，该值已随公开仓库泄露，任何人都能伪造 token。\n" +
                "* 本次启动已改用随机密钥代替（重启后已登录用户需重新登录）。\n" +
                "* 正式环境请在 .env 里设置 QUIZZY_JWT_SECRET：\n" +
                "*   python -c \"import secrets;print(secrets.token_hex(32))\"\n" +
                "****************************************************************************");
        return random;
    }

    /**
     * 解析出来的载荷：用户 id + token 版本号。见 {@link #parseToken(String)}。
     */
    public record TokenPayload(Long userId, int tokenVersion) {
    }

    /**
     * 签发 token。`tv` 是 token 版本号，与 `user.token_version` 比对（见 ADR 0027）——
     * 改密码 / 退出所有设备把库里的值 +1，所有旧 token 下一次请求就失效。
     */
    public String generateToken(Long userId, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public long getExpireMillis() {
        return expireMillis;
    }

    /**
     * 解析 token，失败返回 null。
     *
     * <p>⚠️ 不带 `tv` 的旧 token 按**版本 0** 处理——切换那一刻签出去的存量 token 因此照常可用
     * （库里的默认值也是 0），不会把所有人踢下线。
     */
    public TokenPayload parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            Object rawVersion = claims.get(CLAIM_TOKEN_VERSION);
            int tokenVersion = rawVersion instanceof Number number ? number.intValue() : 0;
            return new TokenPayload(userId, tokenVersion);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("invalid jwt token: {}", e.getMessage());
            return null;
        }
    }
}
