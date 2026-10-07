package com.quizzy.module.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quizzy.common.BusinessException;
import com.quizzy.common.PageResult;
import com.quizzy.common.ResultCode;
import com.quizzy.common.util.AnswerUtil;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.entity.Tag;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.category.mapper.TagMapper;
import com.quizzy.module.category.service.CategoryService;
import com.quizzy.module.category.service.TagService;
import com.quizzy.module.favorite.mapper.FavoriteFolderQuestionMapper;
import com.quizzy.module.question.converter.QuestionConverter;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionQueryDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.entity.QuestionOption;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.mapper.QuestionOptionMapper;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.question.vo.OptionVO;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.question.vo.QuestionVO;
import com.quizzy.module.question.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionStatMapper questionStatMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;
    private final CategoryService categoryService;
    private final QuestionConverter questionConverter;
    private final FavoriteFolderQuestionMapper favoriteFolderQuestionMapper;

    public PageResult<QuestionListItemVO> page(QuestionQueryDTO query, Long userId) {
        long pageNo = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

        LambdaQueryWrapper<Question> wrapper = buildFilters(query, userId);
        if (wrapper == null) {
            // 条件本身指向空集（例如勾的标签一道题都没打过），不必再查库
            return PageResult.of(Collections.emptyList(), 0, pageNo, pageSize);
        }
        wrapper.orderByDesc(Question::getId);

        IPage<Question> mpPage = questionMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<Question> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), mpPage.getTotal(), pageNo, pageSize);
        }

        List<Long> questionIds = records.stream().map(Question::getId).toList();
        Map<Long, List<TagVO>> tagsByQuestion = loadTags(questionIds);
        Map<Long, String> categoryNames = loadCategoryNames(records.stream().map(Question::getCategoryId).toList());
        Set<Long> wrongIds = new HashSet<>(selectWrongQuestionIds(userId));
        // 收藏标记只查**当前页**那几十个 id（与错题标记的全量捞取不同，见 Mapper 上的说明）
        Set<Long> favoriteIds = new HashSet<>(
                favoriteFolderQuestionMapper.selectFavoriteQuestionIds(userId, questionIds));

        List<QuestionListItemVO> items = new ArrayList<>();
        for (Question question : records) {
            QuestionListItemVO item = new QuestionListItemVO();
            fillBase(item, question, userId, categoryNames);
            item.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
            item.setInWrongBook(wrongIds.contains(question.getId()));
            item.setFavorited(favoriteIds.contains(question.getId()));
            items.add(item);
        }
        return PageResult.of(items, mpPage.getTotal(), pageNo, pageSize);
    }

    public QuestionVO detail(Long id, Long userId) {
        Question question = questionMapper.selectById(id);
        assertVisible(question, userId);
        QuestionVO vo = questionConverter.toVO(question);
        vo.setAnswers(AnswerUtil.split(question.getAnswer()));
        vo.setEditable(isOwner(question, userId));
        vo.setFavorited(!favoriteFolderQuestionMapper
                .selectFolderIdsByQuestion(userId, question.getId()).isEmpty());
        vo.setOptions(loadOptions(question.getId()));
        vo.setTags(loadTags(List.of(question.getId())).getOrDefault(question.getId(), List.of()));
        if (question.getCategoryId() != null) {
            Category category = categoryMapper.selectById(question.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        QuestionStat stat = selectStat(userId, question.getId());
        if (stat != null) {
            vo.setAnswerCount(stat.getAnswerCount());
            vo.setCorrectCount(stat.getCorrectCount());
            vo.setInWrongBook(stat.getInWrongBook() != null && stat.getInWrongBook() == 1);
        }
        return vo;
    }

    /**
     * 这道题对当前用户可见吗（公开题或自己的题）？不可见的按「不存在」处理（404），
     * 与 {@link #assertVisible} 同一口径。
     *
     * <p>收藏等**跨模块**的动作靠它守住可见性——公开的那份规则只此一处，别在别处重写一遍。
     */
    public void requireVisible(Long questionId, Long userId) {
        assertVisible(questionMapper.selectById(questionId), userId);
    }

    /**
     * 一批题目对当前用户是否都可见。**一次查询**判完，别在循环里逐题查。
     *
     * <p>题库页的批量动作（导出的 id、收藏夹的批量加入）都走它。
     */
    public void requireVisible(Collection<Long> questionIds, Long userId) {
        List<Long> distinct = questionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先选题目");
        }
        Long visible = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .in(Question::getId, distinct)
                .and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId)));
        if (visible == null || visible != distinct.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "有题目不存在或无权访问");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(QuestionSaveDTO dto, Long userId) {
        validate(dto);
        Question question = new Question();
        boolean update = dto.getId() != null;
        Long previousCategoryId = null;
        if (update) {
            Question existing = questionMapper.selectById(dto.getId());
            assertVisible(existing, userId);
            assertCanEdit(existing, userId);
            question.setId(existing.getId());
            question.setOwnerId(existing.getOwnerId());
            previousCategoryId = existing.getCategoryId();
        } else {
            question.setOwnerId(userId);
        }
        question.setType(dto.getType());
        question.setStem(dto.getStem().trim());
        question.setAnalysis(StringUtils.hasText(dto.getAnalysis()) ? dto.getAnalysis() : null);
        question.setDifficulty(dto.getDifficulty() == null ? com.quizzy.module.question.enums.Difficulty.MEDIUM : dto.getDifficulty());
        question.setAnswer(AnswerUtil.join(dto.getAnswers()));
        // 分类：优先用 id，没有 id 时按名字解析（同名复用、否则新建，与 tags 的做法一致）
        Long categoryId = resolveCategoryId(dto);
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());
        question.setCategoryId(categoryId);

        if (update) {
            questionMapper.updateById(question);
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId()));
            questionMapper.deleteTags(question.getId());
            // 分类被换走 → 旧分类可能就此没人引用，交给自动清理
            if (!Objects.equals(previousCategoryId, categoryId)) {
                categoryService.pruneIfOrphan(previousCategoryId);
            }
        } else {
            questionMapper.insert(question);
        }

        int sort = 0;
        for (OptionDTO option : dto.getOptions()) {
            QuestionOption entity = new QuestionOption();
            entity.setQuestionId(question.getId());
            entity.setLabel(option.getLabel().trim().toUpperCase());
            entity.setContent(option.getContent().trim());
            entity.setSort(sort++);
            questionOptionMapper.insert(entity);
        }
        for (Long tagId : tagService.resolveIds(dto.getTags())) {
            questionMapper.insertTag(question.getId(), tagId);
        }
        return question.getId();
    }

    /**
     * 导出用：加载完整题目信息，**不分页**。
     *
     * <p>两个入口是**互斥**的：
     *
     * <ul>
     *   <li>传了 {@code ids} —— 只导出这些题（仍受范围限制）。此时筛选条件**不参与**，
     *       否则会出现「明明指定了 id 却导不出来」这种排查起来很费解的局面；
     *   <li>没传 {@code ids} —— 按与列表页**同一套**筛选条件导出全部匹配的题目。
     * </ul>
     *
     * <p>可见性始终由 {@code scope} 兜底（默认「公开题或自己的题」），所以接口无法被用来
     * 导出别人的私有题——这一点与列表页共用 {@link #buildFilters}，不是巧合。
     */
    public List<QuestionVO> findForExport(QuestionQueryDTO query, Long userId, List<Long> ids) {
        LambdaQueryWrapper<Question> wrapper;
        if (!CollectionUtils.isEmpty(ids)) {
            wrapper = new LambdaQueryWrapper<>();
            applyScope(wrapper, query.getScope(), userId);
            wrapper.in(Question::getId, ids);
        } else {
            wrapper = buildFilters(query, userId);
            if (wrapper == null) {
                return List.of();
            }
        }
        wrapper.orderByAsc(Question::getId);
        List<Question> questions = questionMapper.selectList(wrapper);
        List<QuestionVO> result = new ArrayList<>();
        for (Question question : questions) {
            QuestionVO vo = questionConverter.toVO(question);
            vo.setAnswers(AnswerUtil.split(question.getAnswer()));
            vo.setOptions(loadOptions(question.getId()));
            vo.setTags(loadTags(List.of(question.getId())).getOrDefault(question.getId(), List.of()));
            if (question.getCategoryId() != null) {
                Category category = categoryMapper.selectById(question.getCategoryId());
                vo.setCategoryName(category == null ? null : category.getName());
            }
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        Question question = questionMapper.selectById(id);
        assertVisible(question, userId);
        assertCanEdit(question, userId);
        questionMapper.deleteById(id);
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, id));
        questionMapper.deleteTags(id);
        questionStatMapper.delete(new LambdaQueryWrapper<QuestionStat>().eq(QuestionStat::getQuestionId, id));
        // 收藏关联跟着硬删：与上面那条「删题硬删作答统计」同一个口径，不留指向已删题的悬挂行（ADR 0029）
        favoriteFolderQuestionMapper.deleteByQuestion(id);
        // 这道题可能是旧分类最后的引用
        categoryService.pruneIfOrphan(question.getCategoryId());
    }

    /**
     * 解析出这道题该挂到哪个分类。
     *
     * <p>有 id 就用 id（存在性在 {@link #validate} 里已校验）；没有 id 但有名字 →
     * 按名字查，**同名复用、否则新建**。于是「想用一个还不存在的分类」与「保存题目」
     * 在**同一个事务**里完成——不会出现「只建了分类、题目没建成」的残局。
     * 这与 {@code tags} 的处理是一致的（标签也是保存题目时按名字自动建）。
     *
     * <p>⚠️ **空白名字按「没填分类」处理**，不报错：它与「不传 {@code categoryId}」等价，
     * 而题目有没有分类在界面上一眼可见，静默忽略的危害很小；反过来把空白当错误，
     * 会让「保存」被拒得莫名其妙。名字超长（&gt;64）仍然报 400——那才是真填错了。
     */
    private Long resolveCategoryId(QuestionSaveDTO dto) {
        if (dto.getCategoryId() != null) {
            return dto.getCategoryId();
        }
        if (StringUtils.hasText(dto.getCategoryName())) {
            return categoryService.resolveByName(dto.getCategoryName()).getId();
        }
        return null;
    }

    /**
     * 把「范围 + 筛选条件」拼成查询条件。**列表与导出共用这一份**，两处口径才不可能漂。
     *
     * @return 条件本身指向空集时返回 {@code null}——调用方据此直接返回空结果，
     *         而不是去查一个必然为空的库
     */
    private LambdaQueryWrapper<Question> buildFilters(QuestionQueryDTO query, Long userId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        applyScope(wrapper, query.getScope(), userId);

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Question::getStem, query.getKeyword().trim());
        }
        if (query.getType() != null) {
            wrapper.eq(Question::getType, query.getType());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(Question::getDifficulty, query.getDifficulty());
        }
        applyCategoryFilter(wrapper, query);
        if (!CollectionUtils.isEmpty(query.getTagIds())) {
            List<Long> ids = questionMapper.selectQuestionIdsByTagIds(query.getTagIds());
            if (ids.isEmpty()) {
                return null;
            }
            wrapper.in(Question::getId, ids);
        }
        if (Boolean.TRUE.equals(query.getOnlyWrong())) {
            List<Long> wrongIds = selectWrongQuestionIds(userId);
            if (wrongIds.isEmpty()) {
                return null;
            }
            wrapper.in(Question::getId, wrongIds);
        }
        List<Long> favoriteIds = resolveFavoriteFilter(query, userId);
        if (favoriteIds != null) {
            if (favoriteIds.isEmpty()) {
                return null;
            }
            wrapper.in(Question::getId, favoriteIds);
        }
        return wrapper;
    }

    /**
     * 收藏筛选。返回 {@code null} 表示「没启用收藏筛选」，返回空列表表示「这个条件一道题都命不中」。
     *
     * <p>⚠️「全部收藏」（{@code anyFavorite}）一旦为真就**忽略** {@code favoriteFolderIds}——
     * 两者是包含关系而非并列关系（「在任意夹里」当然包含「在这些夹里」）。形状与分类那套
     * （多选 + 「未分类」哨兵）一致，写法上也照它那样避免把 OR 漏到外层去。
     */
    private List<Long> resolveFavoriteFilter(QuestionQueryDTO query, Long userId) {
        if (Boolean.TRUE.equals(query.getAnyFavorite())) {
            return favoriteFolderQuestionMapper.selectAllFavoriteQuestionIds(userId);
        }
        if (CollectionUtils.isEmpty(query.getFavoriteFolderIds())) {
            return null;
        }
        return favoriteFolderQuestionMapper.selectQuestionIdsByFolders(userId, query.getFavoriteFolderIds());
    }

    /**
     * 分类筛选：多选，且「未分类」是一个独立的可选项，它和具体分类之间是**或**的关系。
     *
     * <p>⚠️ 必须整体包在 {@code and(...)} 里。直接写成 {@code in(...).or().isNull(...)} 的话，
     * 那个 {@code or} 会和**外面所有 AND 条件平级**，于是
     * 「范围=我的 AND 关键词=xx AND 分类 IN (...) OR 未分类」——最后一项会把前两项全部绕过去，
     * 变成「只要没分类就命中」。这类括号错误不会报错，只会静默多返回数据。
     *
     * <p>一个都不勾 = 不按分类筛（等于「全部分类」），此时不产生任何条件。
     */
    private void applyCategoryFilter(LambdaQueryWrapper<Question> wrapper, QuestionQueryDTO query) {
        List<Long> categoryIds = query.getCategoryIds();
        boolean hasCategories = !CollectionUtils.isEmpty(categoryIds);
        boolean includeUncategorized = Boolean.TRUE.equals(query.getUncategorized());
        if (!hasCategories && !includeUncategorized) {
            return;
        }
        if (hasCategories && includeUncategorized) {
            wrapper.and(w -> w.in(Question::getCategoryId, categoryIds).or().isNull(Question::getCategoryId));
        } else if (hasCategories) {
            wrapper.in(Question::getCategoryId, categoryIds);
        } else {
            wrapper.isNull(Question::getCategoryId);
        }
    }

    private void applyScope(LambdaQueryWrapper<Question> wrapper, String scope, Long userId) {
        if ("mine".equalsIgnoreCase(scope)) {
            wrapper.eq(Question::getOwnerId, userId);
        } else if ("public".equalsIgnoreCase(scope)) {
            wrapper.isNull(Question::getOwnerId);
        } else {
            wrapper.and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId));
        }
    }

    private void validate(QuestionSaveDTO dto) {
        Set<String> labels = new LinkedHashSet<>();
        for (OptionDTO option : dto.getOptions()) {
            if (!labels.add(option.getLabel().trim().toUpperCase())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "选项标号重复：" + option.getLabel());
            }
        }
        List<String> answers = dto.getAnswers().stream()
                .map(a -> a.trim().toUpperCase())
                .toList();
        if (answers.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "正确答案不能为空");
        }
        for (String answer : answers) {
            if (!labels.contains(answer)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "正确答案 " + answer + " 不在选项中");
            }
        }
        if (dto.getType() != QuestionType.MULTI && answers.size() > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单选题与判断题只能有一个正确答案");
        }
        if (dto.getType() == QuestionType.JUDGE && dto.getOptions().size() != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "判断题必须且只能有 2 个选项");
        }
        if (dto.getCategoryId() != null && categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类不存在");
        }
    }

    private void assertVisible(Question question, Long userId) {
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        if (question.getOwnerId() != null && !question.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在或无权访问");
        }
    }

    private void assertCanEdit(Question question, Long userId) {
        if (!isOwner(question, userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "公开题只读，无法修改");
        }
    }

    private boolean isOwner(Question question, Long userId) {
        return question.getOwnerId() != null && question.getOwnerId().equals(userId);
    }

    private List<OptionVO> loadOptions(Long questionId) {
        return questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, questionId)
                        .orderByAsc(QuestionOption::getSort))
                .stream()
                .map(o -> new OptionVO(o.getLabel(), o.getContent()))
                .toList();
    }

    private Map<Long, List<TagVO>> loadTags(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = questionMapper.selectTagIdsBatch(questionIds);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = rows.stream()
                .map(row -> ((Number) row.get("tagId")).longValue())
                .collect(Collectors.toSet());
        Map<Long, String> tagNames = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));

        Map<Long, List<TagVO>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long questionId = ((Number) row.get("questionId")).longValue();
            Long tagId = ((Number) row.get("tagId")).longValue();
            String name = tagNames.get(tagId);
            if (name != null) {
                result.computeIfAbsent(questionId, key -> new ArrayList<>()).add(new TagVO(tagId, name));
            }
        }
        return result;
    }

    private Map<Long, String> loadCategoryNames(List<Long> categoryIds) {
        List<Long> distinct = categoryIds.stream().filter(id -> id != null).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private List<Long> selectWrongQuestionIds(Long userId) {
        List<QuestionStat> stats = questionStatMapper.selectList(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getInWrongBook, 1));
        return stats.stream().map(QuestionStat::getQuestionId).toList();
    }

    private QuestionStat selectStat(Long userId, Long questionId) {
        return questionStatMapper.selectOne(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getQuestionId, questionId));
    }

    private void fillBase(QuestionListItemVO item, Question question, Long userId, Map<Long, String> categoryNames) {
        item.setId(question.getId());
        item.setType(question.getType());
        item.setStem(question.getStem());
        item.setDifficulty(question.getDifficulty());
        item.setScore(question.getScore());
        item.setOwnerId(question.getOwnerId());
        item.setEditable(isOwner(question, userId));
        item.setCategoryName(question.getCategoryId() == null ? null : categoryNames.get(question.getCategoryId()));
    }
}
