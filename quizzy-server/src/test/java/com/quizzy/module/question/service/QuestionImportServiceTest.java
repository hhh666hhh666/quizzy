package com.quizzy.module.question.service;

import com.quizzy.common.BusinessException;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.service.PaperService;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.vo.ImportResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportServiceTest {

    @Test
    @DisplayName("跳过错误行：一行失败不影响其余行，并返回逐行错误报告")
    void shouldSkipInvalidRowAndKeepOthers() {
        QuestionService fakeQuestionService = new QuestionService(null, null, null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                if ("第二题".equals(dto.getStem())) {
                    throw new BusinessException("单选题与判断题只能有一个正确答案");
                }
                return 1L;
            }
        };
        QuestionImportService importService = new QuestionImportService(fakeQuestionService, null, null);

        ImportResultVO result = importService.importJson(List.of(validQuestion("第一题"), invalidQuestion("第二题")), 1L, null);

        assertEquals(2, result.total());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failed().size());
        assertEquals(2, result.failed().get(0).row());
        assertTrue(result.failed().get(0).reason().contains("只能有一个正确答案"));
    }

    @Test
    @DisplayName("未知题型在转换阶段就标记为错误行")
    void unknownTypeShouldBeReported() {
        QuestionImportService importService = new QuestionImportService(null, null, null);
        QuestionImportDTO dto = validQuestion("第一题");
        dto.setType("填空题");

        ImportResultVO result = importService.importJson(List.of(dto), 1L, null);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failed().size());
        assertEquals(1, result.failed().get(0).row());
        assertTrue(result.failed().get(0).reason().contains("未知题型"));
    }

    @Test
    @DisplayName("中文题型别名与多种分隔符都能正确解析")
    void shouldParseAliasesAndSeparators() {
        QuestionService fakeQuestionService = new QuestionService(null, null, null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                assertEquals(2, dto.getAnswers().size());
                assertTrue(dto.getAnswers().contains("A"));
                assertTrue(dto.getAnswers().contains("C"));
                assertEquals(QuestionType.MULTI, dto.getType());
                return 1L;
            }
        };
        QuestionImportService importService = new QuestionImportService(fakeQuestionService, null, null);

        QuestionImportDTO dto = validQuestion("第一题");
        dto.setType("多选");
        dto.setAnswer(List.of("A", "C"));
        dto.setOptions(List.of(option("A", "甲"), option("B", "乙"), option("C", "丙")));

        ImportResultVO result = importService.importJson(List.of(dto), 1L, null);

        assertEquals(1, result.successCount());
    }

    // ---------- 导入时顺带建卷（ADR 0026） ----------

    @Test
    @DisplayName("给了卷名：本次**成功**导入的题装进一张新固定卷，失败的题不进去")
    void createsOneFixedPaperFromSuccessfulRows() {
        QuestionService fakeQuestionService = new QuestionService(null, null, null, null, null, null, null, null, null) {
            private long seq = 100;

            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                if ("第二题".equals(dto.getStem())) {
                    throw new BusinessException("单选题与判断题只能有一个正确答案");
                }
                return seq++;
            }
        };
        List<PaperSaveDTO> built = new ArrayList<>();
        PaperService fakePaperService = new PaperService(null, null, null, null, null, null) {
            @Override
            public Long save(PaperSaveDTO dto, Long userId) {
                built.add(dto);
                return 999L;
            }
        };

        ImportResultVO result = new QuestionImportService(fakeQuestionService, null, fakePaperService)
                .importJson(List.of(validQuestion("第一题"), invalidQuestion("第二题")), 7L, "MySQL");

        assertEquals(1, result.successCount());
        assertEquals(999L, result.paperId());
        assertEquals(1, built.size(), "只该建一张卷");
        PaperSaveDTO paper = built.get(0);
        assertEquals("MySQL", paper.getTitle());
        assertEquals(PaperMode.FIXED, paper.getMode());
        // 关键：只装成功的那一道（第二题是错误行），不是把整批 id 都塞进去
        assertEquals(List.of(100L), paper.getQuestionIds());
    }

    @Test
    @DisplayName("没给卷名 / 一题都没成功：都不建卷")
    void doesNotCreatePaperWithoutTitleOrWithoutQuestions() {
        List<PaperSaveDTO> built = new ArrayList<>();
        PaperService fakePaperService = new PaperService(null, null, null, null, null, null) {
            @Override
            public Long save(PaperSaveDTO dto, Long userId) {
                built.add(dto);
                return 1L;
            }
        };
        QuestionService okService = new QuestionService(null, null, null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                return 1L;
            }
        };
        QuestionService alwaysFailing = new QuestionService(null, null, null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                throw new BusinessException("就是不行");
            }
        };

        // ① 没给卷名：题照常导入，但根本不碰建卷这条路
        ImportResultVO noTitle = new QuestionImportService(okService, null, fakePaperService)
                .importJson(List.of(validQuestion("第一题")), 1L, null);
        assertEquals(1, noTitle.successCount());
        assertNull(noTitle.paperId());

        // ② 给了卷名但全军覆没：建一张空卷对用户没有意义，只会污染试卷列表
        ImportResultVO allFailed = new QuestionImportService(alwaysFailing, null, fakePaperService)
                .importJson(List.of(validQuestion("第一题")), 1L, "MySQL");
        assertEquals(0, allFailed.successCount());
        assertNull(allFailed.paperId());

        assertTrue(built.isEmpty(), "这两种情况都不该真的调用建卷");
    }

    private QuestionImportDTO validQuestion(String stem) {
        QuestionImportDTO dto = new QuestionImportDTO();
        dto.setType("single");
        dto.setStem(stem);
        dto.setAnswer(List.of("A"));
        dto.setOptions(List.of(option("A", "选项A"), option("B", "选项B")));
        return dto;
    }

    private QuestionImportDTO invalidQuestion(String stem) {
        QuestionImportDTO dto = new QuestionImportDTO();
        dto.setType("single");
        dto.setStem(stem);
        dto.setAnswer(List.of("A", "B"));
        dto.setOptions(List.of(option("A", "选项A"), option("B", "选项B")));
        return dto;
    }

    private OptionDTO option(String label, String content) {
        OptionDTO option = new OptionDTO();
        option.setLabel(label);
        option.setContent(content);
        return option;
    }
}
