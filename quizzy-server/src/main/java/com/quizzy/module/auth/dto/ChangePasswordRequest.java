package com.quizzy.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 改密码。密码长度沿用注册时的口径（6-64），不另立一套。
 *
 * <p>要求旧密码：否则会话被劫持时，攻击者可以直接改密码把主人锁在外面。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 个字符")
    private String newPassword;
}
