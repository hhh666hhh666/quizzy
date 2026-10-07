package com.quizzy.module.auth.vo;

/**
 * 当前用户对自己可见的公开信息。
 *
 * <p>`avatar` 是 data URL（可能几十 KB）；界面拿不到它时用前端生成的默认头像，见 ADR 0028。
 */
public record UserVO(Long id, String username, String nickname, String avatar) {
}
