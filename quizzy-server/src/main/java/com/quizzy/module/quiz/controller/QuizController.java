package com.quizzy.module.quiz.controller;

import com.quizzy.common.PageResult;
import com.quizzy.common.Result;
import com.quizzy.module.quiz.dto.AnswerDTO;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.service.QuizService;
import com.quizzy.module.quiz.vo.AnswerResultVO;
import com.quizzy.module.quiz.vo.SessionResultVO;
import com.quizzy.module.quiz.vo.SessionVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "答题")
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "发起一次答题，返回会话 id")
    @PostMapping("/start")
    public Result<Long> start(@Valid @RequestBody QuizStartDTO dto) {
        return Result.success(quizService.start(dto, UserContext.requireUserId()));
    }

    @Operation(summary = "会话详情（含题目与已作答内容，用于续答）")
    @GetMapping("/sessions/{id}")
    public Result<SessionVO> detail(@PathVariable Long id) {
        return Result.success(quizService.detail(id, UserContext.requireUserId()));
    }

    @Operation(summary = "提交单题答案并即时判分")
    @PostMapping("/sessions/{id}/answer")
    public Result<AnswerResultVO> answer(@PathVariable Long id, @Valid @RequestBody AnswerDTO dto) {
        return Result.success(quizService.answer(id, dto, UserContext.requireUserId()));
    }

    @Operation(summary = "结束答题并结算")
    @PostMapping("/sessions/{id}/finish")
    public Result<SessionResultVO> finish(@PathVariable Long id) {
        return Result.success(quizService.finish(id, UserContext.requireUserId()));
    }

    @Operation(summary = "放弃本次答题")
    @PostMapping("/sessions/{id}/abandon")
    public Result<Void> abandon(@PathVariable Long id) {
        quizService.abandon(id, UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "历史答题列表")
    @GetMapping("/sessions")
    public Result<PageResult<SessionVO>> list(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status) {
        return Result.success(quizService.list(page, size, status, UserContext.requireUserId()));
    }

    @Operation(summary = "答题结果回顾")
    @GetMapping("/sessions/{id}/result")
    public Result<SessionResultVO> result(@PathVariable Long id) {
        return Result.success(quizService.result(id, UserContext.requireUserId()));
    }
}
