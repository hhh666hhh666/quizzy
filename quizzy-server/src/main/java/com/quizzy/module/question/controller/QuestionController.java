package com.quizzy.module.question.controller;

import com.quizzy.common.PageResult;
import com.quizzy.common.Result;
import com.quizzy.module.question.dto.QuestionQueryDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.service.QuestionService;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.question.vo.QuestionVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题目")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Validated
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "题目分页列表")
    @GetMapping
    public Result<PageResult<QuestionListItemVO>> page(@Valid @ModelAttribute QuestionQueryDTO query) {
        return Result.success(questionService.page(query, UserContext.requireUserId()));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(id, UserContext.requireUserId()));
    }

    @Operation(summary = "新建题目")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody QuestionSaveDTO dto) {
        return Result.success(questionService.save(dto, UserContext.requireUserId()));
    }

    @Operation(summary = "修改题目")
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveDTO dto) {
        dto.setId(id);
        return Result.success(questionService.save(dto, UserContext.requireUserId()));
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id, UserContext.requireUserId());
        return Result.success();
    }
}
