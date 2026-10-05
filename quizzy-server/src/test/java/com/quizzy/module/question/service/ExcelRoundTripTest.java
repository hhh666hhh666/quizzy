package com.quizzy.module.question.service;

import com.alibaba.excel.EasyExcel;
import com.quizzy.module.question.dto.QuestionExcelRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 解析的烟测——**它存在的唯一理由是补一个洞**。
 *
 * <p>此前 xlsx 的读与写**没有任何测试会真的跑到**：
 * `QuestionImportServiceTest` 只测导入**校验**（不碰解析），
 * 端到端 `07-import-export` 走的是 JSON 粘贴那一条路。
 * 于是 easyexcel / POI 这条链一旦被版本升级弄坏，**CI 全绿也发现不了**。
 *
 * <p>⚠️ 刻意**不碰数据库**：只做「写一个小 xlsx → 再读回来」的往返，
 * 走的是与生产代码**同一条** easyexcel 路径——导入的读
 * （{@code EasyExcel.read(...).head(QuestionExcelRow.class)}）与导出的写都用
 * {@link QuestionExcelRow} 按列索引映射。所以它留在**单元层**（`*Test`），
 * 不需要 Docker、几秒跑完，也就不会拖慢任何一条门禁。
 *
 * <p>它真正想钉住的：有人升 POI / commons-compress 时，这里必须红——
 * 而不是等到线上有人传了个 xlsx 才发现导入挂了。
 *
 * @see QuestionImportService
 */
class ExcelRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void 写出去的xlsx能按列索引再读回来() {
        Path file = tempDir.resolve("round-trip.xlsx");

        QuestionExcelRow row = new QuestionExcelRow();
        row.setType("single");
        row.setStem("Excel 烟测的题干");
        row.setOptionA("甲");
        row.setOptionB("乙");
        row.setAnswer("A");
        row.setAnalysis("解析");
        row.setDifficulty("easy");
        row.setScore("2");
        row.setCategory("分类");
        row.setTags("标签");

        EasyExcel.write(file.toFile()).head(QuestionExcelRow.class).sheet("题目").doWrite(List.of(row));

        // 这一步才真正解压（commons-compress）与解析 OOXML（poi-ooxml）
        List<QuestionExcelRow> read = EasyExcel.read(file.toFile())
                .head(QuestionExcelRow.class)
                .sheet()
                .doReadSync();

        assertThat(read).hasSize(1);
        QuestionExcelRow back = read.get(0);
        assertThat(back.getStem()).isEqualTo("Excel 烟测的题干");
        assertThat(back.getOptionA()).isEqualTo("甲");
        assertThat(back.getOptionB()).isEqualTo("乙");
        assertThat(back.getAnswer()).isEqualTo("A");
        // 刻意不断言 score 的字面量：数值单元格读回字符串可能是 "2" 也可能是 "2.0"，
        // 那是 easyexcel 的事，不是这条烟测要钉的东西。
        assertThat(back.getScore()).isNotNull();
    }
}
