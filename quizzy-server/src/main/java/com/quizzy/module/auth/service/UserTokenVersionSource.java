package com.quizzy.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.module.auth.entity.User;
import com.quizzy.module.auth.mapper.UserMapper;
import com.quizzy.security.TokenVersionSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link TokenVersionSource} 的实现：从 `user` 表读版本号。
 *
 * <p>⚠️ 只 select `token_version` 这一列。用 `selectById` 会把整行取回来，**包括可能几十 KB 的 avatar**——
 * 而这段代码在每一个带 token 的请求上都会跑（这是 docs/adr/0027 里写明的代价，别在这里再放大它）。
 */
@Component
@RequiredArgsConstructor
public class UserTokenVersionSource implements TokenVersionSource {

    private final UserMapper userMapper;

    @Override
    public Integer currentVersion(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getTokenVersion)
                .eq(User::getId, userId));
        if (user == null) {
            return null;
        }
        // 防御性：库里该列是 NOT NULL DEFAULT 0，理论上取不到 null。
        Integer version = user.getTokenVersion();
        return version == null ? 0 : version;
    }
}
