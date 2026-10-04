package com.quizzy.module.category.controller;

import com.quizzy.common.Result;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.service.CategoryService;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口。
 *
 * <p>⚠️ 这里**只提供 list 与「改分类名」**，刻意没有新建与删除：
 *
 * <ul>
 *   <li>**新建**随「保存题目」发生（{@code QuestionSaveDTO#categoryName}）——
 *       同一个事务里完成，不会出现「只建了分类、题目没建成」；
 *   <li>**删除**由系统自动做——一个分类不再被任何题目引用时自动清掉
 *       （{@code CategoryService#pruneIfOrphan}）。
 * </ul>
 *
 * <p>原因记在 {@code docs/todo/2026-10-04-TODO-共享分类缺少归属校验.md}：
 * 分类是全体共用的，而服务已对公网开放，任何注册用户都能「主动删除」共享分类是不可接受的。
 * 于是改成：**你只能动自己的归属，动不了别人的；没人用的分类由系统回收。**
 */
@Tag(name = "分类")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    /**
     * 改分类名。
     *
     * <p>⚠️ 语义不是「原地改名」：分类是共享的，原地改会让**所有人**的题目都换名字。
     * 这里是「**我把我的题挪到另一个分类**」——按名字解析目标（同名复用、即合并；否则新建），
     * 然后只迁移调用者自己的题目；旧分类若因此无人引用，由系统自动清掉。
     */
    @Operation(summary = "改分类名（只迁移自己的题目）")
    @PutMapping("/{id}")
    public Result<Category> moveTo(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return Result.success(categoryService.moveTo(id, request.getName(), UserContext.requireUserId()));
    }

    @Data
    public static class CategoryRequest {
        private String name;
    }
}
