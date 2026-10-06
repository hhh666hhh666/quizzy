package com.quizzy.module.paper.vo;

/**
 * 往固定卷追加题目后的结果。
 *
 * @param added 本次真正新增的题量（已在卷里被忽略的不算）
 * @param total 追加之后这张卷的总题量
 */
public record PaperAppendResultVO(int added, int total) {
}
