package com.quizzy.module.question.controller;

import com.quizzy.common.BusinessException;
import com.quizzy.common.Result;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.dto.QuestionQueryDTO;
import com.quizzy.module.question.service.QuestionImportService;
import com.quizzy.module.question.vo.ImportResultVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    /**
     * JSON 批量导入。
     *
     * <p>⚠️ body **仍是裸数组**（ADR 0006）：卷名这类元信息走查询参数，不进 body——
     * 理由是它与「题目内容」是两回事（ADR 0006 Amendment 1）。
     *
     * @param paperTitle 可选。给了就顺带把本次**成功导入**的题装进一张新固定卷（ADR 0026）
     */
    @Operation(summary = "JSON 批量导入（可选 paperTitle：顺带建一张固定卷）")
    @PostMapping("/import/json")
    public Result<ImportResultVO> importJson(@Valid @RequestBody List<QuestionImportDTO> items,
                                             @RequestParam(required = false) String paperTitle) {
        return Result.success(importService.importJson(items, UserContext.requireUserId(), paperTitle));
    }

    /** Excel 导入。{@code paperTitle} 语义同 {@link #importJson}，走 multipart 的普通表单字段。 */
    @Operation(summary = "Excel 导入（可选 paperTitle：顺带建一张固定卷）")
    @PostMapping("/import/excel")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String paperTitle) {
        assertExcel(file);
        try {
            return Result.success(importService.importExcel(file.getInputStream(), UserContext.requireUserId(), paperTitle));
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

    /**
     * 导出题目。
     *
     * <p>与列表页**共用同一套筛选参数**（{@code QuestionQueryDTO}），所以界面上筛完再导出，
     * 拿到的就是筛出来的那一批。⚠️ 只跟随**筛选条件**，不跟随**分页**——导出全部匹配的题目。
     *
     * <p>传了 {@code ids} 则只导出这些题，此时筛选条件不参与（两个入口互斥，理由见
     * {@code QuestionService#findForExport}）。
     */
    @Operation(summary = "导出题目（跟随筛选条件；传 ids 则按 id 精确导出）")
    @GetMapping("/export")
    public void export(@Valid @ModelAttribute QuestionQueryDTO query,
                       @RequestParam(defaultValue = "excel") String format,
                       @RequestParam(required = false) List<Long> ids,
                       HttpServletResponse response) throws IOException {
        Long userId = UserContext.requireUserId();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if ("json".equalsIgnoreCase(format)) {
            prepareJsonResponse(response, "quizzy-questions-" + stamp + ".json");
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(importService.exportJson(query, userId, ids)));
        } else {
            prepareExcelResponse(response, "quizzy-questions-" + stamp + ".xlsx");
            importService.exportExcel(query, userId, ids, response.getOutputStream());
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
