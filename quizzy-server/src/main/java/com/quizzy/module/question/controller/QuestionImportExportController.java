package com.quizzy.module.question.controller;

import com.quizzy.common.BusinessException;
import com.quizzy.common.Result;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.service.QuestionImportService;
import com.quizzy.module.question.vo.ImportResultVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "题目导入导出")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionImportExportController {

    private final QuestionImportService importService;

    @Operation(summary = "JSON 批量导入")
    @PostMapping("/import/json")
    public Result<ImportResultVO> importJson(@Valid @RequestBody List<QuestionImportDTO> items) {
        return Result.success(importService.importJson(items, UserContext.requireUserId()));
    }

    @Operation(summary = "Excel 导入")
    @PostMapping("/import/excel")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        assertExcel(file);
        try {
            return Result.success(importService.importExcel(file.getInputStream(), UserContext.requireUserId()));
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + e.getMessage());
        }
    }

    @Operation(summary = "下载 Excel 导入模板")
    @GetMapping("/template/excel")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "quizzy-question-template.xlsx");
        importService.writeTemplate(response.getOutputStream());
    }

    @Operation(summary = "导出题目（ids 为空表示导出全部可见题目）")
    @GetMapping("/export")
    public void export(@RequestParam(defaultValue = "excel") String format,
                       @RequestParam(required = false) List<Long> ids,
                       HttpServletResponse response) throws IOException {
        Long userId = UserContext.requireUserId();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if ("json".equalsIgnoreCase(format)) {
            prepareJsonResponse(response, "quizzy-questions-" + stamp + ".json");
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(importService.exportJson(ids, userId)));
        } else {
            prepareExcelResponse(response, "quizzy-questions-" + stamp + ".xlsx");
            importService.exportExcel(ids, userId, response.getOutputStream());
        }
    }

    private void assertExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException("仅支持 .xlsx / .xls 文件");
        }
    }

    private void prepareExcelResponse(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        writeFileName(response, filename);
    }

    private void prepareJsonResponse(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        writeFileName(response, filename);
    }

    private void writeFileName(HttpServletResponse response, String filename) {
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }
}
