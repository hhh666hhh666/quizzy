package com.quizzy.module.paper.controller;

import com.quizzy.common.PageResult;
import com.quizzy.common.Result;
import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.paper.service.PaperService;
import com.quizzy.module.paper.vo.PaperVO;
import com.quizzy.module.paper.vo.QuestionPreviewVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "试卷")
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @Operation(summary = "试卷列表")
    @GetMapping
    public Result<PageResult<PaperVO>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(paperService.page(page, size, UserContext.requireUserId()));
    }

    @Operation(summary = "试卷详情")
    @GetMapping("/{id}")
    public Result<PaperVO> detail(@PathVariable Long id) {
        return Result.success(paperService.detail(id, UserContext.requireUserId()));
    }

    @Operation(summary = "新建试卷")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PaperSaveDTO dto) {
        return Result.success(paperService.save(dto, UserContext.requireUserId()));
    }

    @Operation(summary = "修改试卷")
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody PaperSaveDTO dto) {
        dto.setId(id);
        return Result.success(paperService.save(dto, UserContext.requireUserId()));
    }

    @Operation(summary = "删除试卷")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperService.delete(id, UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "规则预览抽题")
    @PostMapping("/preview")
    public Result<List<QuestionPreviewVO>> preview(@RequestBody PaperRuleDTO rule) {
        return Result.success(paperService.preview(rule, UserContext.requireUserId()));
    }
}
