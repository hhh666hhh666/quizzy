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
import com.quizzy.module.paper.entity.Paper;
import com.quizzy.module.paper.entity.PaperQuestion;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.mapper.PaperMapper;
import com.quizzy.module.paper.mapper.PaperQuestionMapper;
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

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionStatMapper questionStatMapper;
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
        if (dto.getMode() == PaperMode.FIXED) {
            if (CollectionUtils.isEmpty(dto.getQuestionIds())) {
                throw new BusinessException("固定卷必须选择题目");
            }
            paper.setRuleJson(null);
            paper.setQuestionCount(dto.getQuestionIds().size());
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
            for (Long questionId : dto.getQuestionIds()) {
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
        if (rule.getCategoryId() != null) {
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
