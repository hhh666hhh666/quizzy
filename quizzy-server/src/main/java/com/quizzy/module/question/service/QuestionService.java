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
import com.quizzy.module.category.service.TagService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final QuestionConverter questionConverter;

    public PageResult<QuestionListItemVO> page(QuestionQueryDTO query, Long userId) {
        long pageNo = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

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
        if (query.getCategoryId() != null) {
            wrapper.eq(Question::getCategoryId, query.getCategoryId());
        }
        if (!CollectionUtils.isEmpty(query.getTagIds())) {
            List<Long> ids = questionMapper.selectQuestionIdsByTagIds(query.getTagIds());
            if (ids.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, pageNo, pageSize);
            }
            wrapper.in(Question::getId, ids);
        }
        if (Boolean.TRUE.equals(query.getOnlyWrong())) {
            List<Long> wrongIds = selectWrongQuestionIds(userId);
            if (wrongIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, pageNo, pageSize);
            }
            wrapper.in(Question::getId, wrongIds);
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

        List<QuestionListItemVO> items = new ArrayList<>();
        for (Question question : records) {
            QuestionListItemVO item = new QuestionListItemVO();
            fillBase(item, question, userId, categoryNames);
            item.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
            item.setInWrongBook(wrongIds.contains(question.getId()));
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

    @Transactional(rollbackFor = Exception.class)
    public Long save(QuestionSaveDTO dto, Long userId) {
        validate(dto);
        Question question = new Question();
        boolean update = dto.getId() != null;
        if (update) {
            Question existing = questionMapper.selectById(dto.getId());
            assertVisible(existing, userId);
            assertCanEdit(existing, userId);
            question.setId(existing.getId());
            question.setOwnerId(existing.getOwnerId());
        } else {
            question.setOwnerId(userId);
        }
        question.setType(dto.getType());
        question.setStem(dto.getStem().trim());
        question.setAnalysis(StringUtils.hasText(dto.getAnalysis()) ? dto.getAnalysis() : null);
        question.setDifficulty(dto.getDifficulty() == null ? com.quizzy.module.question.enums.Difficulty.MEDIUM : dto.getDifficulty());
        question.setAnswer(AnswerUtil.join(dto.getAnswers()));
        question.setScore(dto.getScore() == null ? 1 : dto.getScore());
        question.setCategoryId(dto.getCategoryId());

        if (update) {
            questionMapper.updateById(question);
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId()));
            questionMapper.deleteTags(question.getId());
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
     * 导出用：按 id 列表或全部可见题目加载完整题目信息。
     */
    public List<QuestionVO> findForExport(Long userId, List<Long> ids) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (!CollectionUtils.isEmpty(ids)) {
            wrapper.in(Question::getId, ids);
            wrapper.and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId));
        } else {
            wrapper.and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId));
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
