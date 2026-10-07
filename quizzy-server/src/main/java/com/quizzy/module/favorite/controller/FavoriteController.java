package com.quizzy.module.favorite.controller;

import com.quizzy.common.PageResult;
import com.quizzy.common.Result;
import com.quizzy.module.favorite.dto.FavoriteFolderIdsDTO;
import com.quizzy.module.favorite.dto.FavoriteFolderSaveDTO;
import com.quizzy.module.favorite.dto.FavoriteQuestionsDTO;
import com.quizzy.module.favorite.service.FavoriteService;
import com.quizzy.module.favorite.vo.FavoriteFolderVO;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 收藏夹。模型见 docs/adr/0030。
 *
 * <p>两处容易混淆的边界：
 *
 * <ul>
 *   <li>**题库页的收藏筛选**走 {@code GET /api/questions?anyFavorite=…&favoriteFolderIds=…}，
 *       与其它筛选条件共用同一套实现——那是**题目视角**，排序仍是题目 id 倒序；
 *   <li>**收藏夹页面的右列**走本类的 {@code GET /api/favorites/questions}——那是**收藏视角**，
 *       按最近收藏的排最前，还要跨夹去重（同一题可能在多个夹里）。排序维度只存在于关联表上，
 *       塞进通用的题目查询会让它多出「参数 A 只在参数 B 存在时才有意义」的耦合。
 * </ul>
 *
 * <p>两处**输出形状仍是一份**（都返回题目列表项，组装复用 {@code QuestionService}），
 * 变的只是顺序与分页。
 */
@Tag(name = "收藏夹")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏夹列表（带题目数，按最近有新题进来倒序）")
    @GetMapping("/folders")
    public Result<List<FavoriteFolderVO>> listFolders() {
        return Result.success(favoriteService.listFolders(UserContext.requireUserId()));
    }

    @Operation(summary = "新建收藏夹")
    @PostMapping("/folders")
    public Result<FavoriteFolderVO> createFolder(@Valid @RequestBody FavoriteFolderSaveDTO dto) {
        return Result.success(favoriteService.createFolder(dto.getName(), UserContext.requireUserId()));
    }

    @Operation(summary = "收藏夹改名（默认收藏夹也可以改）")
    @PutMapping("/folders/{folderId}")
    public Result<Void> renameFolder(@PathVariable Long folderId, @Valid @RequestBody FavoriteFolderSaveDTO dto) {
        favoriteService.renameFolder(folderId, dto.getName(), UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "删除收藏夹（默认收藏夹不可删；只把题从它里面移出）")
    @DeleteMapping("/folders/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId) {
        favoriteService.deleteFolder(folderId, UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "把一批题目加入某个收藏夹（只加不减）")
    @PostMapping("/folders/{folderId}/questions")
    public Result<Void> addQuestions(@PathVariable Long folderId, @Valid @RequestBody FavoriteQuestionsDTO dto) {
        favoriteService.addQuestionsToFolder(folderId, dto.getQuestionIds(), UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "某道题从某个收藏夹移出")
    @DeleteMapping("/folders/{folderId}/questions/{questionId}")
    public Result<Void> removeFromFolder(@PathVariable Long folderId, @PathVariable Long questionId) {
        favoriteService.removeFromFolder(folderId, questionId, UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "收藏夹里的题目（不传 folderId = 全部收藏），按最近收藏的排最前")
    @GetMapping("/questions")
    public Result<PageResult<QuestionListItemVO>> pageQuestions(@RequestParam(required = false) Long folderId,
                                                                @RequestParam(defaultValue = "1") Long page,
                                                                @RequestParam(defaultValue = "10") Long size) {
        return Result.success(favoriteService.pageQuestions(folderId, page, size, UserContext.requireUserId()));
    }

    @Operation(summary = "收藏（落到默认收藏夹，返回它供界面提示）")
    @PostMapping("/questions/{questionId}")
    public Result<FavoriteFolderVO> favorite(@PathVariable Long questionId) {
        return Result.success(favoriteService.favorite(questionId, UserContext.requireUserId()));
    }

    @Operation(summary = "取消收藏（从所有收藏夹移出，返回被移出的夹供界面撤销）")
    @DeleteMapping("/questions/{questionId}")
    public Result<List<Long>> unfavorite(@PathVariable Long questionId) {
        return Result.success(favoriteService.unfavorite(questionId, UserContext.requireUserId()));
    }

    @Operation(summary = "这道题现在在哪些收藏夹里")
    @GetMapping("/questions/{questionId}/folders")
    public Result<List<Long>> foldersOfQuestion(@PathVariable Long questionId) {
        return Result.success(favoriteService.foldersOfQuestion(questionId, UserContext.requireUserId()));
    }

    @Operation(summary = "覆盖式设置题目所属收藏夹（不传或为空 = 留在默认收藏夹）")
    @PutMapping("/questions/{questionId}/folders")
    public Result<Void> setFoldersOfQuestion(@PathVariable Long questionId,
                                             @RequestBody FavoriteFolderIdsDTO dto) {
        favoriteService.setFoldersOfQuestion(questionId, dto.getFolderIds(), UserContext.requireUserId());
        return Result.success();
    }

    @Operation(summary = "用收藏的题开一次练习（不传 folderId 表示全部收藏）")
    @PostMapping("/practice")
    public Result<Long> practice(@RequestParam(required = false) Long folderId,
                                @RequestParam(defaultValue = "20") int count) {
        return Result.success(favoriteService.practice(folderId, count, UserContext.requireUserId()));
    }
}
