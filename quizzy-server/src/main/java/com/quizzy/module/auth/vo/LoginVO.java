package com.quizzy.module.auth.vo;

import lombok.Builder;

@Builder
public record LoginVO(String token, long expireMillis, UserVO user) {
}
