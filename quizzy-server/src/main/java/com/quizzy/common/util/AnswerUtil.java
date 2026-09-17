package com.quizzy.common.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 答案字符串与集合互转工具。存储格式为逗号分隔的 label，如 {@code A,C}。
 */
public final class AnswerUtil {

    private AnswerUtil() {
    }

    public static List<String> split(String answer) {
        List<String> result = new ArrayList<>();
        if (answer == null) {
            return result;
        }
        for (String part : answer.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed.toUpperCase());
            }
        }
        return result;
    }

    public static String join(List<String> answers) {
        Set<String> deduped = new LinkedHashSet<>();
        if (answers != null) {
            for (String answer : answers) {
                String trimmed = answer == null ? "" : answer.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    deduped.add(trimmed);
                }
            }
        }
        return String.join(",", deduped);
    }

    /**
     * 忽略顺序与大小写比较两份答案是否完全一致。
     */
    public static boolean same(String left, String right) {
        List<String> leftParts = split(left);
        List<String> rightParts = split(right);
        if (leftParts.size() != rightParts.size()) {
            return false;
        }
        return new LinkedHashSet<>(leftParts).containsAll(rightParts)
                && new LinkedHashSet<>(rightParts).containsAll(leftParts);
    }
}
