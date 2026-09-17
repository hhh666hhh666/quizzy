package com.quizzy.module.question.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {

    SINGLE("单选题"),
    MULTI("多选题"),
    JUDGE("判断题");

    private final String description;

    /**
     * 该题型允许的答案个数上限，0 表示不限（多选题）。
     */
    public int maxAnswerCount() {
        return switch (this) {
            case SINGLE, JUDGE -> 1;
            case MULTI -> Integer.MAX_VALUE;
        };
    }
}
