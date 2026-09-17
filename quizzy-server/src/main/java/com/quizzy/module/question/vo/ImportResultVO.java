package com.quizzy.module.question.vo;

import java.util.ArrayList;
import java.util.List;

public record ImportResultVO(int total, int successCount, List<ImportErrorVO> failed) {

    public static ImportResultVO of(int total, int successCount, List<ImportErrorVO> failed) {
        return new ImportResultVO(total, successCount, failed == null ? new ArrayList<>() : failed);
    }
}
