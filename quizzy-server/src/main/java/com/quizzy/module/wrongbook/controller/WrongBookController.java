package com.quizzy.module.wrongbook.controller;

import com.quizzy.common.PageResult;
import com.quizzy.common.Result;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.wrongbook.service.WrongBookService;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "错题本")
@RestController
@RequestMapping("/api/wrong-book")
@RequiredArgsConstructor
public class WrongBookController {

    private final WrongBookService wrongBookService;

    @Operation(summary = "错题列表")
    @GetMapping
    public Result<PageResult<QuestionListItemVO>> page(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.success(wrongBookService.page(page, size, UserContext.requireUserId()));
    }

    @Operation(summary = "手动移出错题本")
    @DeleteMapping("/{questionId}")
    public Result<Void> remove(@PathVariable Long questionId) {
        wrongBookService.remove(questionId, UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "发起错题练习")
    @PostMapping("/practice")
    public Result<Long> practice(@RequestParam(defaultValue = "20") int count) {
        return Result.success(wrongBookService.practice(count, UserContext.requireUserId()));
    }
}
