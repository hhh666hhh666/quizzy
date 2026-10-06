package com.quizzy.module.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizzy.common.BusinessException;
import com.quizzy.common.PageResult;
import com.quizzy.common.ResultCode;
import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.paper.entity.Paper;
import com.quizzy.module.paper.entity.PaperQuestion;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.mapper.PaperMapper;
import com.quizzy.module.paper.mapper.PaperQuestionMapper;
import com.quizzy.module.paper.vo.PaperAppendResultVO;
import com.quizzy.module.paper.vo.PaperVO;
import com.quizzy.module.paper.vo.QuestionPreviewVO;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionStatMapper questionStatMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    public PageResult<PaperVO> page(long pageNo, long pageSize, Long userId) {
        IPage<Paper> mpPage = paperMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getOwnerId, userId)
                        .orderByDesc(Paper::getId));
        List<PaperVO> items = mpPage.getRecords().stream().map(paper -> toVO(paper, true)).toList();
        return PageResult.of(items, mpPage.getTotal(), pageNo, pageSize);
    }

    public PaperVO detail(Long id, Long userId) {
        return toVO(requireOwned(id, userId), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(PaperSaveDTO dto, Long userId) {
        boolean update = dto.getId() != null;
        Paper paper = update ? requireOwned(dto.getId(), userId) : new Paper();
        paper.setTitle(dto.getTitle().trim());
        paper.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription() : null);
        paper.setMode(dto.getMode());
        List<Long> questionIds = List.of();
        if (dto.getMode() == PaperMode.FIXED) {
            // 允许**空卷**：先建卷、之后再往里加题（ADR 0026）。空卷不能发起作答，
            // 由 QuizService#start 兜底报「没有符合要求的题目」。
            // 顺带去重——paper_question 上有 (paper_id, question_id) 唯一键，
            // 入参带重复 id 会让整次保存在约束上炸掉（题库页多选很容易撞）。
            questionIds = distinctIds(dto.getQuestionIds());
            // 只校验**新加进来**的题：已在卷里的（比如后来被自己软删的题）允许继续挂着，
            // 否则「编辑一张含已删题的卷」会突然保存不了。
            // ⚠️ 缺了这道校验，任何人只要猜到 id（自增的），就能把别人的私有题加进自己的卷，
            //    再通过作答把题干与解析读出来——可见性规则见 docs/design/数据模型.md。
            Set<Long> alreadyInPaper = update ? relationQuestionIds(paper.getId()) : Set.of();
            requireVisibleQuestions(questionIds.stream().filter(id -> !alreadyInPaper.contains(id)).toList(), userId);
            paper.setRuleJson(null);
            paper.setQuestionCount(questionIds.size());
        } else {
            if (dto.getRule() == null) {
                throw new BusinessException("规则卷必须配置抽题规则");
            }
            paper.setRuleJson(writeRule(dto.getRule()));
            paper.setQuestionCount(dto.getRule().getCount() == null ? 20 : dto.getRule().getCount());
        }
        if (update) {
            paperMapper.updateById(paper);
            paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>()
                    .eq(PaperQuestion::getPaperId, paper.getId()));
        } else {
            paper.setOwnerId(userId);
            paperMapper.insert(paper);
        }
        if (dto.getMode() == PaperMode.FIXED) {
            int sort = 0;
            for (Long questionId : questionIds) {
                PaperQuestion relation = new PaperQuestion();
                relation.setPaperId(paper.getId());
                relation.setQuestionId(questionId);
                relation.setSort(sort++);
                paperQuestionMapper.insert(relation);
            }
        }
        return paper.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        requireOwned(id, userId);
        paperMapper.deleteById(id);
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, id));
    }

    /**
     * 往一张**固定卷**里追加题目——题库页「加入已有试卷」的落点。
     *
     * <p>语义是**并入**而不是替换：已经在卷里的题会被忽略（重复调用不会越加越多），
     * 入参里的重复 id 也只算一次。要整体替换题目列表，走 {@link #save}。
     *
     * <p>之所以不并进 {@code save}：{@code save} 是「全量替换」（先清空关系再重建），
     * 而「加入」的用户预期是**保留原有的**，两者语义相反，共用一个入口迟早出事。
     *
     * @return 本次真正新增的题量，以及追加后的总题量
     */
    @Transactional(rollbackFor = Exception.class)
    public PaperAppendResultVO appendQuestions(Long paperId, List<Long> questionIds, Long userId) {
        Paper paper = requireOwned(paperId, userId);
        if (paper.getMode() != PaperMode.FIXED) {
            throw new BusinessException("只有固定卷才能追加题目");
        }
        List<Long> wanted = distinctIds(questionIds);
        if (wanted.isEmpty()) {
            throw new BusinessException("请选择要加入的题目");
        }
        requireVisibleQuestions(wanted, userId);

        List<PaperQuestion> existing = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
        Set<Long> inPaper = existing.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toSet());
        // 接在末尾：sort 取现有最大值 +1，别用 size——中途删过题的话 size 会与最大值错开、排到别人前面去
        int sort = existing.stream().mapToInt(PaperQuestion::getSort).max().orElse(-1) + 1;

        int added = 0;
        for (Long questionId : wanted) {
            if (!inPaper.add(questionId)) {
                continue;
            }
            PaperQuestion relation = new PaperQuestion();
            relation.setPaperId(paperId);
            relation.setQuestionId(questionId);
            relation.setSort(sort++);
            paperQuestionMapper.insert(relation);
            added++;
        }

        // inPaper 现在 = 原有的 + 新增的，正好是追加后的总题量
        paper.setQuestionCount(inPaper.size());
        paperMapper.updateById(paper);
        return new PaperAppendResultVO(added, inPaper.size());
    }

    /**
     * 断言这些题对当前用户**可见**（公开题，或自己的题），否则拒绝。
     *
     * <p>⚠️ 少了它，只要猜到 id，就能把**别人的私有题**加进自己的卷，再通过作答把题干与解析读出来——
     * id 是自增的，猜得起。可见性规则见 docs/design/数据模型.md。
     */
    private void requireVisibleQuestions(List<Long> questionIds, Long userId) {
        if (questionIds.isEmpty()) {
            return;
        }
        Long visible = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .in(Question::getId, questionIds)
                .and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId)));
        if (visible == null || visible != questionIds.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "有题目不存在或不属于你");
        }
    }

    /** 这张卷现在已经关联了哪些题（用于「已挂在卷上的题不再校验可见性」）。 */
    private Set<Long> relationQuestionIds(Long paperId) {
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, paperId))
                .stream()
                .map(PaperQuestion::getQuestionId)
                .collect(Collectors.toSet());
    }

    /** 去重且保留原顺序——固定卷的题目顺序是有意义的（作答顺序）；顺带滤掉 null。 */
    private static List<Long> distinctIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    /**
     * 规则卷预览：按规则抽取题目但不落库。
     */
    public List<QuestionPreviewVO> preview(PaperRuleDTO rule, Long userId) {
        return selectQuestionsByRule(rule, userId).stream()
                .map(q -> new QuestionPreviewVO(q.getId(), q.getType(), q.getStem(), q.getDifficulty(), q.getScore()))
                .toList();
    }

    /**
     * 解析一次作答实际使用的题目 id 列表。
     */
    public List<Long> resolveQuestionIds(Paper paper, Long userId) {
        if (paper.getMode() == PaperMode.FIXED) {
            List<PaperQuestion> relations = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, paper.getId())
                            .orderByAsc(PaperQuestion::getSort));
            return relations.stream().map(PaperQuestion::getQuestionId).toList();
        }
        PaperRuleDTO rule = readRule(paper.getRuleJson());
        return selectQuestionsByRule(rule, userId).stream().map(Question::getId).toList();
    }

    private List<Question> selectQuestionsByRule(PaperRuleDTO rule, Long userId) {
        if (rule == null) {
            throw new BusinessException("规则卷缺少抽题规则");
        }
        int count = rule.getCount() == null ? 20 : Math.min(Math.max(rule.getCount(), 1), 200);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .and(w -> w.isNull(Question::getOwnerId).or().eq(Question::getOwnerId, userId));
        // ⚠️ 分类可能已经被自动清理掉了（无人引用时自动删除）。
        //    这时让「按分类筛」这条条件**失效**（跳过），而不是让整张卷一道题都抽不到——
        //    否则用户只会看到「没有符合要求的题目」，完全不知道原因。
        //    规则卷作答与「预览抽题结果」共用这里，行为自动一致。
        if (rule.getCategoryId() != null && categoryMapper.selectById(rule.getCategoryId()) != null) {
            wrapper.eq(Question::getCategoryId, rule.getCategoryId());
        }
        if (!CollectionUtils.isEmpty(rule.getTypes())) {
            wrapper.in(Question::getType, rule.getTypes());
        }
        if (!CollectionUtils.isEmpty(rule.getDifficulties())) {
            wrapper.in(Question::getDifficulty, rule.getDifficulties());
        }
        if (!CollectionUtils.isEmpty(rule.getTagIds())) {
            List<Long> ids = questionMapper.selectQuestionIdsByTagIds(rule.getTagIds());
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Question::getId, ids);
        }
        if (rule.getExcludeRecentDays() != null && rule.getExcludeRecentDays() > 0) {
            List<Long> recentIds = selectRecentQuestionIds(userId, rule.getExcludeRecentDays());
            if (!recentIds.isEmpty()) {
                wrapper.notIn(Question::getId, recentIds);
            }
        }
        wrapper.last("ORDER BY RAND() LIMIT " + count);
        return new ArrayList<>(questionMapper.selectList(wrapper));
    }

    private List<Long> selectRecentQuestionIds(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<QuestionStat> stats = questionStatMapper.selectList(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .isNotNull(QuestionStat::getLastAnswerTime)
                .ge(QuestionStat::getLastAnswerTime, since));
        return stats.stream().map(QuestionStat::getQuestionId).toList();
    }

    private Paper requireOwned(Long id, Long userId) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null || !userId.equals(paper.getOwnerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "试卷不存在");
        }
        return paper;
    }

    private PaperVO toVO(Paper paper, boolean withQuestionIds) {
        PaperVO vo = new PaperVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setDescription(paper.getDescription());
        vo.setMode(paper.getMode());
        vo.setQuestionCount(paper.getQuestionCount());
        if (paper.getMode() == PaperMode.RULE) {
            vo.setRule(readRule(paper.getRuleJson()));
        }
        if (withQuestionIds && paper.getMode() == PaperMode.FIXED) {
            vo.setQuestionIds(paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, paper.getId())
                            .orderByAsc(PaperQuestion::getSort))
                    .stream()
                    .map(PaperQuestion::getQuestionId)
                    .toList());
        }
        return vo;
    }

    private String writeRule(PaperRuleDTO rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            throw new BusinessException("抽题规则序列化失败");
        }
    }

    private PaperRuleDTO readRule(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PaperRuleDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("抽题规则解析失败");
        }
    }
}
