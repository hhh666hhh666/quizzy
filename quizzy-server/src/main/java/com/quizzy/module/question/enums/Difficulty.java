package com.quizzy.module.question.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {

    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");

    private final String description;

    public static Difficulty ofOrDefault(String value) {
        if (value == null) {
            return MEDIUM;
        }
        for (Difficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(value)) {
                return difficulty;
            }
        }
        return MEDIUM;
    }
}
