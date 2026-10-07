package com.quizzy.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注销账号（不可逆）。要当前密码做二次确认，见 docs/adr/0029。
 */
@Data
public class DeleteAccountRequest {

    @NotBlank(message = "密码不能为空")
    private String password;
}
