package com.quizzy.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;
import com.quizzy.module.auth.dto.ChangePasswordRequest;
import com.quizzy.module.auth.dto.DeleteAccountRequest;
import com.quizzy.module.auth.dto.LoginRequest;
import com.quizzy.module.auth.dto.RegisterRequest;
import com.quizzy.module.auth.dto.UpdateProfileRequest;
import com.quizzy.module.auth.entity.User;
import com.quizzy.module.auth.mapper.UserMapper;
import com.quizzy.module.auth.vo.LoginVO;
import com.quizzy.module.auth.vo.UserVO;
import com.quizzy.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AccountDeletionService accountDeletionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname()
                : request.getUsername());
        // 显式写上，别依赖建表默认值：下面 buildLoginVO 要拿它签 token（见 ADR 0027）。
        user.setTokenVersion(0);
        userMapper.insert(user);
        return buildLoginVO(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildLoginVO(user);
    }

    public UserVO currentUser(Long userId) {
        return toUserVO(requireUser(userId));
    }

    /**
     * 改昵称 / 改头像。**不含登录名**——`username` 不可改是刻意的（见《需求范围》的「用户」一条）。
     *
     * <p>用 LambdaUpdateWrapper 而不是 `updateById`：后者默认跳过 null 字段，
     * 于是「把头像改回默认的那张」就永远写不进去。
     */
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        requireUser(userId);
        String avatar = AvatarDataUrl.normalize(request.getAvatar());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, request.getNickname().trim())
                .set(User::getAvatar, avatar));
        return toUserVO(requireUser(userId));
    }

    /**
     * 改密码：验旧密码 → 换新哈希 → 版本号 +1 → **给当前设备换发新 token**。
     *
     * <p>结果就是「本机无感、其他设备全部掉线」：其他设备手里的旧 token 下一次请求即 401。
     */
    public LoginVO changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(request.getNewPassword()))
                .set(User::getTokenVersion, tokenVersionOf(user) + 1));
        return buildLoginVO(requireUser(userId));
    }

    /**
     * 退出所有设备——**包括本机**。调用方（前端）收到成功后应清掉本地 token 并回登录页。
     */
    public void logoutAllDevices(Long userId) {
        User user = requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getTokenVersion, tokenVersionOf(user) + 1));
    }

    /**
     * 注销账号（不可逆）。要当前密码做二次确认；清理范围见 {@link AccountDeletionService}。
     */
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不正确");
        }
        accountDeletionService.deleteEverythingOf(userId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        return user;
    }

    /** 库里的默认值是 0，但刚 insert 完的内存对象是 null——统一在这里兜住。 */
    static int tokenVersionOf(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private LoginVO buildLoginVO(User user) {
        return LoginVO.builder()
                .token(jwtUtil.generateToken(user.getId(), tokenVersionOf(user)))
                .expireMillis(jwtUtil.getExpireMillis())
                .user(toUserVO(user))
                .build();
    }

    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }
}
