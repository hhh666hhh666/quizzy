package com.quizzy.module.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;
import com.quizzy.module.favorite.entity.FavoriteFolder;
import com.quizzy.module.favorite.mapper.FavoriteFolderMapper;
import com.quizzy.module.favorite.mapper.FavoriteFolderQuestionMapper;
import com.quizzy.module.favorite.vo.FavoriteFolderVO;
import com.quizzy.module.question.service.QuestionService;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 收藏夹。模型与取舍见 docs/adr/0030，三条最容易搞错的地方先写在前面：
 *
 * <ol>
 *   <li>「**收藏**」等于「这道题**在至少一个夹里**」，不存在独立于收藏夹的收藏状态，
 *       也没有「未分组」——所以「取消收藏」= 从所有夹移出；
 *   <li>每个用户天然有个**默认收藏夹**（{@link #requireDefaultFolder} 按需创建，存量用户不必迁移），
 *       它不可删、可改名，于是它的名字**不许写死进任何界面文案**；
 *   <li>「修改收藏夹」是**覆盖**语义且**空选 = 留在默认夹**；而题库页的批量「加入」是**只加不减**——
 *       两个入口的名字不同（「修改」/「加入」），行为也不同，别把它们当成同一个动作。
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    /**
     * 默认收藏夹的**初始名字**。
     *
     * <p>⚠️ 用户可以改它（ADR 0030 允许改名），所以这句话只该出现在这里当创建时的初始值，
     * **不许**出现在任何返回给界面的文案里——界面要显示就用接口返回的那个名字。
     */
    private static final String DEFAULT_FOLDER_NAME = "默认收藏夹";

    /** 抽题上限，与错题重练对齐 */
    private static final int MAX_PRACTICE_COUNT = 200;

    private final FavoriteFolderMapper folderMapper;
    private final FavoriteFolderQuestionMapper folderQuestionMapper;
    private final QuestionService questionService;
    private final QuizService quizService;

    public List<FavoriteFolderVO> listFolders(Long userId) {
        return folderMapper.selectFoldersWithStat(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public FavoriteFolderVO createFolder(String rawName, Long userId) {
        String name = normalizeName(rawName);
        assertNameAvailable(userId, name, null);
        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setIsDefault(0);
        folderMapper.insert(folder);
        return toVO(folder);
    }

    /** 改名。**默认收藏夹也允许改**——所以「默认收藏夹」这五个字不进界面文案。 */
    @Transactional(rollbackFor = Exception.class)
    public void renameFolder(Long folderId, String rawName, Long userId) {
        FavoriteFolder folder = requireFolder(folderId, userId);
        String name = normalizeName(rawName);
        assertNameAvailable(userId, name, folderId);
        folder.setName(name);
        folderMapper.updateById(folder);
    }

    /**
     * 删夹：只把题从它里面移出。题目若因此不属于任何夹，就不再是收藏（ADR 0030）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId, Long userId) {
        FavoriteFolder folder = requireFolder(folderId, userId);
        if (isDefault(folder)) {
            // 默认夹删了，「随手收藏」就无处可落——这条是硬约束，不是可以商量的默认值。
            throw new BusinessException(ResultCode.BAD_REQUEST, "默认收藏夹不能删除");
        }
        folderQuestionMapper.deleteByFolder(folderId);
        folderMapper.deleteById(folderId);
    }

    /**
     * 收藏（短按星标）：落到默认收藏夹。返回默认夹，界面据此弹「已加入『X』」的横幅——
     * 静默成功会让主人不知道题去哪了。
     */
    @Transactional(rollbackFor = Exception.class)
    public FavoriteFolderVO favorite(Long questionId, Long userId) {
        questionService.requireVisible(questionId, userId);
        FavoriteFolder folder = requireDefaultFolder(userId);
        folderQuestionMapper.insertIgnore(folder.getId(), questionId);
        return toVO(folder);
    }

    /**
     * 取消收藏（再短按一次）：从**所有**夹移出。
     *
     * @return 被移出的夹 id —— 界面据此提供一次「撤销」原样加回，纯客户端实现、不新增接口
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> unfavorite(Long questionId, Long userId) {
        questionService.requireVisible(questionId, userId);
        List<Long> folderIds = folderQuestionMapper.selectFolderIdsByQuestion(userId, questionId);
        folderQuestionMapper.deleteByUserAndQuestion(userId, questionId);
        return folderIds;
    }

    /** 这道题现在在哪些夹里——「修改收藏夹」面板与撤销都要用。 */
    public List<Long> foldersOfQuestion(Long questionId, Long userId) {
        return folderQuestionMapper.selectFolderIdsByQuestion(userId, questionId);
    }

    /**
     * 覆盖式设置「这道题属于哪些夹」。
     *
     * <p>⚠️ {@code folderIds} 为空 = **留在默认夹**，而不是取消收藏——见 docs/adr/0030。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setFoldersOfQuestion(Long questionId, List<Long> folderIds, Long userId) {
        questionService.requireVisible(questionId, userId);
        folderQuestionMapper.deleteByUserAndQuestion(userId, questionId);
        if (CollectionUtils.isEmpty(folderIds)) {
            folderQuestionMapper.insertIgnore(requireDefaultFolder(userId).getId(), questionId);
            return;
        }
        for (FavoriteFolder folder : requireFolders(folderIds, userId)) {
            folderQuestionMapper.insertIgnore(folder.getId(), questionId);
        }
    }

    /**
     * 批量**加入**某个夹（只加不减）——题库页批量条用。
     *
     * <p>与「修改收藏夹」的覆盖语义刻意不同：批量动作若会「把没勾的夹里那些题挪走」，
     * 一次点击就能静默改掉几十道题的归属。批量只做加法。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addQuestionsToFolder(Long folderId, List<Long> questionIds, Long userId) {
        FavoriteFolder folder = requireFolder(folderId, userId);
        questionService.requireVisible(questionIds, userId);
        for (Long questionId : questionIds) {
            folderQuestionMapper.insertIgnore(folder.getId(), questionId);
        }
    }

    /** 从某一个夹里移出（题若因此不属于任何夹，就不再是收藏）。 */
    @Transactional(rollbackFor = Exception.class)
    public void removeFromFolder(Long folderId, Long questionId, Long userId) {
        requireFolder(folderId, userId);
        folderQuestionMapper.deleteByFolderAndQuestion(folderId, questionId);
    }

    /** 用收藏的题开一次练习；{@code folderId} 为空表示「全部收藏」。 */
    public Long practice(Long folderId, Integer count, Long userId) {
        QuizStartDTO dto = new QuizStartDTO();
        dto.setSourceType(SourceType.FAVORITE);
        dto.setFolderId(folderId);
        dto.setCount(count == null || count <= 0 ? 20 : Math.min(count, MAX_PRACTICE_COUNT));
        return quizService.start(dto, userId);
    }

    /**
     * 默认收藏夹，**按需创建**。
     *
     * <p>按需而不是注册时预建，是为了让存量账号不必做数据迁移（ADR 0030）。
     * 并发下两个请求可能同时发现「没有默认夹」，靠 {@code (user_id, name)} 的唯一键兜底：
     * 后一个插入失败就重读一次。
     */
    private FavoriteFolder requireDefaultFolder(Long userId) {
        FavoriteFolder existing = selectDefaultFolder(userId);
        if (existing != null) {
            return existing;
        }
        FavoriteFolder created = new FavoriteFolder();
        created.setUserId(userId);
        created.setName(availableDefaultName(userId));
        created.setIsDefault(1);
        try {
            folderMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            FavoriteFolder again = selectDefaultFolder(userId);
            if (again == null) {
                throw e;
            }
            return again;
        }
    }

    private FavoriteFolder selectDefaultFolder(Long userId) {
        return folderMapper.selectOne(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .eq(FavoriteFolder::getIsDefault, 1)
                .orderByAsc(FavoriteFolder::getId)
                .last("LIMIT 1"));
    }

    /**
     * 给默认夹挑一个没被占用的初始名字。
     *
     * <p>主人可能在它诞生之前就建过一个叫「默认收藏夹」的普通夹，那就顺延到「默认收藏夹 2」……
     */
    private String availableDefaultName(Long userId) {
        for (int i = 1; i <= 50; i += 1) {
            String candidate = i == 1 ? DEFAULT_FOLDER_NAME : DEFAULT_FOLDER_NAME + " " + i;
            if (!nameTaken(userId, candidate)) {
                return candidate;
            }
        }
        return DEFAULT_FOLDER_NAME + " " + userId;
    }

    private FavoriteFolder requireFolder(Long folderId, Long userId) {
        FavoriteFolder folder = folderId == null ? null : folderMapper.selectById(folderId);
        if (folder == null || !userId.equals(folder.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收藏夹不存在或无权访问");
        }
        return folder;
    }

    private List<FavoriteFolder> requireFolders(List<Long> folderIds, Long userId) {
        List<Long> distinct = folderIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<FavoriteFolder> folders = folderMapper.selectList(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .in(FavoriteFolder::getId, distinct));
        if (folders.size() != distinct.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收藏夹不存在或无权访问");
        }
        return folders;
    }

    private void assertNameAvailable(Long userId, String name, Long exceptFolderId) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .eq(FavoriteFolder::getName, name);
        if (exceptFolderId != null) {
            wrapper.ne(FavoriteFolder::getId, exceptFolderId);
        }
        Long count = folderMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "已经有一个叫「" + name + "」的收藏夹了");
        }
    }

    private boolean nameTaken(Long userId, String name) {
        Long count = folderMapper.selectCount(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .eq(FavoriteFolder::getName, name));
        return count != null && count > 0;
    }

    private String normalizeName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "收藏夹名字不能为空");
        }
        return rawName.trim();
    }

    private boolean isDefault(FavoriteFolder folder) {
        return folder.getIsDefault() != null && folder.getIsDefault() == 1;
    }

    private FavoriteFolderVO toVO(FavoriteFolder folder) {
        FavoriteFolderVO vo = new FavoriteFolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setIsDefault(isDefault(folder));
        return vo;
    }
}
