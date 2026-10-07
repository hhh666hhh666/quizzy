package com.quizzy.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 改昵称 / 改头像。**不含登录名**——`username` 不可改是刻意的，理由见《需求范围》的「用户」一条。
 *
 * <p>头像这一项是 data URL 字符串：null 或空串表示**改回默认的生成头像**，不是「不改」。
 * 校验与规范化在 {@code AvatarDataUrl}，取舍见 docs/adr/0028。
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最多 32 个字符")
    private String nickname;

    private String avatar;
}
