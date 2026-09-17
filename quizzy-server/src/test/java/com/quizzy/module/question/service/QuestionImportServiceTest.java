package com.quizzy.module.question.service;

import com.quizzy.common.BusinessException;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.vo.ImportResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionImportServiceTest {

    @Test
    @DisplayName("跳过错误行：一行失败不影响其余行，并返回逐行错误报告")
    void shouldSkipInvalidRowAndKeepOthers() {
        QuestionService fakeQuestionService = new QuestionService(null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                if ("第二题".equals(dto.getStem())) {
                    throw new BusinessException("单选题与判断题只能有一个正确答案");
                }
                return 1L;
            }
        };
        QuestionImportService importService = new QuestionImportService(fakeQuestionService, null);

        ImportResultVO result = importService.importJson(List.of(validQuestion("第一题"), invalidQuestion("第二题")), 1L);

        assertEquals(2, result.total());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failed().size());
        assertEquals(2, result.failed().get(0).row());
        assertTrue(result.failed().get(0).reason().contains("只能有一个正确答案"));
    }

    @Test
    @DisplayName("未知题型在转换阶段就标记为错误行")
    void unknownTypeShouldBeReported() {
        QuestionImportService importService = new QuestionImportService(null, null);
        QuestionImportDTO dto = validQuestion("第一题");
        dto.setType("填空题");

        ImportResultVO result = importService.importJson(List.of(dto), 1L);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failed().size());
        assertEquals(1, result.failed().get(0).row());
        assertTrue(result.failed().get(0).reason().contains("未知题型"));
    }

    @Test
    @DisplayName("中文题型别名与多种分隔符都能正确解析")
    void shouldParseAliasesAndSeparators() {
        QuestionService fakeQuestionService = new QuestionService(null, null, null, null, null, null, null) {
            @Override
            public Long save(QuestionSaveDTO dto, Long userId) {
                assertEquals(2, dto.getAnswers().size());
                assertTrue(dto.getAnswers().contains("A"));
                assertTrue(dto.getAnswers().contains("C"));
                assertEquals(QuestionType.MULTI, dto.getType());
                return 1L;
            }
        };
        QuestionImportService importService = new QuestionImportService(fakeQuestionService, null);

        QuestionImportDTO dto = validQuestion("第一题");
        dto.setType("多选");
        dto.setAnswer(List.of("A", "C"));
        dto.setOptions(List.of(option("A", "甲"), option("B", "乙"), option("C", "丙")));

        ImportResultVO result = importService.importJson(List.of(dto), 1L);

        assertEquals(1, result.successCount());
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
