package com.quizzy.module.question.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次导入的结果。
 *
 * @param total        解析出的题目条数（Excel 里题干为空的行不计）
 * @param successCount 成功入库的条数
 * @param failed       逐行失败明细
 * @param paperId      按需创建的固定卷 id；**没建卷时为 {@code null}**
 *                     （⚠️ 接口配了 {@code non_null}，为 null 时这个字段在响应里根本不出现，
 *                     调用方要用 {@code hasNonNull("paperId")} 判断，不能拿 {@code isNull()}）
 */
public record ImportResultVO(int total, int successCount, List<ImportErrorVO> failed, Long paperId) {

    public ImportResultVO {
        failed = failed == null ? new ArrayList<>() : failed;
    }

    public static ImportResultVO of(int total, int successCount, List<ImportErrorVO> failed) {
        return new ImportResultVO(total, successCount, failed, null);
    }

    public static ImportResultVO of(int total, int successCount, List<ImportErrorVO> failed, Long paperId) {
        return new ImportResultVO(total, successCount, failed, paperId);
    }
}
