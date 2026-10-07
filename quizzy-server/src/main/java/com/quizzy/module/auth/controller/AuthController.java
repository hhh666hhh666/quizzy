package com.quizzy.module.auth.controller;

import com.quizzy.common.Result;
import com.quizzy.module.auth.dto.ChangePasswordRequest;
import com.quizzy.module.auth.dto.DeleteAccountRequest;
import com.quizzy.module.auth.dto.LoginRequest;
import com.quizzy.module.auth.dto.RegisterRequest;
import com.quizzy.module.auth.dto.UpdateProfileRequest;
import com.quizzy.module.auth.service.AuthService;
import com.quizzy.module.auth.vo.LoginVO;
import com.quizzy.module.auth.vo.UserVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与账户自助。账户自助四件的取舍见 docs/adr/0027 / 0028 / 0029。
 *
 * <p>⚠️ 「改登录名」这个接口**不存在**，是刻意不做，不是漏了——见《需求范围》的「用户」一条。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser(UserContext.requireUserId()));
    }

    @Operation(summary = "改昵称 / 改头像")
    @PutMapping("/me")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authService.updateProfile(UserContext.requireUserId(), request));
    }

    @Operation(summary = "改密码（本机换发新 token，其他设备立即失效）")
    @PutMapping("/password")
    public Result<LoginVO> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return Result.success(authService.changePassword(UserContext.requireUserId(), request));
    }

    @Operation(summary = "退出所有设备（含本机）")
    @PostMapping("/logout-all")
    public Result<Void> logoutAll() {
        authService.logoutAllDevices(UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "注销账号（不可逆，需当前密码）")
    @PostMapping("/delete-account")
    public Result<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(UserContext.requireUserId(), request);
        return Result.success();
    }
}
