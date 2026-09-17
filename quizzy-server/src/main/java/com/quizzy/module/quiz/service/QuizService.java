package com.quizzy.module.quiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quizzy.common.BusinessException;
import com.quizzy.common.PageResult;
import com.quizzy.common.ResultCode;
import com.quizzy.common.util.AnswerUtil;
import com.quizzy.module.paper.entity.Paper;
import com.quizzy.module.paper.mapper.PaperMapper;
import com.quizzy.module.paper.service.PaperService;
import com.quizzy.module.paper.vo.QuestionPreviewVO;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.entity.QuestionOption;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.mapper.QuestionOptionMapper;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.question.vo.OptionVO;
import com.quizzy.module.quiz.dto.AnswerDTO;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.entity.QuizAnswer;
import com.quizzy.module.quiz.entity.QuizSession;
import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.mapper.QuizAnswerMapper;
import com.quizzy.module.quiz.mapper.QuizSessionMapper;
import com.quizzy.module.quiz.vo.AnswerResultVO;
import com.quizzy.module.quiz.vo.QuizQuestionVO;
import com.quizzy.module.quiz.vo.QuizResultItemVO;
import com.quizzy.module.quiz.vo.SessionResultVO;
import com.quizzy.module.quiz.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {

    /** 连续答对多少次自动移出错题本 */
    private static final int CONSECUTIVE_TO_LEAVE_WRONG_BOOK = 3;

    private final QuizSessionMapper sessionMapper;
    private final QuizAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionStatMapper statMapper;
    private final PaperMapper paperMapper;
    private final PaperService paperService;
    private final ScoreStrategy scoreStrategy;

    @Transactional(rollbackFor = Exception.class)
    public Long start(QuizStartDTO dto, Long userId) {
        List<Long> questionIds;
        String title;
        Long paperId = null;
        switch (dto.getSourceType()) {
            case PAPER -> {
                if (dto.getPaperId() == null) {
                    throw new BusinessException("请选择试卷");
                }
                Paper paper = paperMapper.selectById(dto.getPaperId());
                if (paper == null || !userId.equals(paper.getOwnerId())) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "试卷不存在");
                }
                questionIds = paperService.resolveQuestionIds(paper, userId);
                paperId = paper.getId();
                title = paper.getTitle();
            }
            case QUICK -> {
                if (dto.getRule() == null) {
                    throw new BusinessException("请配置抽题规则");
                }
                questionIds = paperService.preview(dto.getRule(), userId).stream()
                        .map(QuestionPreviewVO::id)
                        .toList();
                title = "快速练习";
            }
            case WRONG_BOOK -> {
                questionIds = selectWrongBookQuestionIds(userId, dto.getCount() == null ? 20 : dto.getCount());
                title = "错题重练";
            }
            default -> throw new BusinessException("不支持的答题来源");
        }
        if (questionIds.isEmpty()) {
            throw new BusinessException("没有符合要求的题目，换个条件试试");
        }

        QuizSession session = new QuizSession();
        session.setUserId(userId);
        session.setPaperId(paperId);
        session.setSourceType(dto.getSourceType());
        session.setTitle(title);
        session.setQuestionCount(questionIds.size());
        session.setCurrentIndex(0);
        session.setTotalScore(0);
        session.setObtainedScore(0);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartTime(LocalDateTime.now());
        sessionMapper.insert(session);

        int sort = 0;
        int totalScore = 0;
        for (Long questionId : questionIds) {
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                continue;
            }
            totalScore += question.getScore() == null ? 1 : question.getScore();
            QuizAnswer answer = new QuizAnswer();
            answer.setSessionId(session.getId());
            answer.setQuestionId(questionId);
            answer.setUserAnswer("");
            answer.setIsCorrect(0);
            answer.setScore(0);
            answer.setSort(sort++);
            answer.setAnsweredAt(LocalDateTime.now());
            answerMapper.insert(answer);
        }
        session.setTotalScore(totalScore);
        sessionMapper.updateById(session);
        return session.getId();
    }

    public SessionVO detail(Long sessionId, Long userId) {
        QuizSession session = requireSession(sessionId, userId);
        List<QuizAnswer> answers = loadAnswers(sessionId);
        Map<Long, Question> questionMap = loadQuestionMap(answers.stream().map(QuizAnswer::getQuestionId).toList());
        SessionVO vo = toSessionVO(session);
        List<QuizQuestionVO> questions = new ArrayList<>();
        for (QuizAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            QuizQuestionVO item = new QuizQuestionVO();
            item.setQuestionId(question.getId());
            item.setIndex(answer.getSort());
            item.setType(question.getType());
            item.setStem(question.getStem());
            item.setScore(question.getScore());
            item.setOptions(loadOptions(question.getId()));
            boolean answered = StringUtils.hasText(answer.getUserAnswer());
            item.setAnswered(answered);
            if (answered) {
                item.setUserAnswers(AnswerUtil.split(answer.getUserAnswer()));
                item.setCorrectAnswers(AnswerUtil.split(question.getAnswer()));
                item.setAnalysis(question.getAnalysis());
                item.setIsCorrect(answer.getIsCorrect() != null && answer.getIsCorrect() == 1);
            }
            questions.add(item);
        }
        vo.setQuestions(questions);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public AnswerResultVO answer(Long sessionId, AnswerDTO dto, Long userId) {
        QuizSession session = requireSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("该答题已经结束");
        }
        QuizAnswer answer = answerMapper.selectOne(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getSessionId, sessionId)
                .eq(QuizAnswer::getQuestionId, dto.getQuestionId()));
        if (answer == null) {
            throw new BusinessException("题目不属于本次答题");
        }
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }

        String userAnswer = AnswerUtil.join(AnswerUtil.split(dto.getAnswer()));
        ScoreStrategy.JudgeResult judged = scoreStrategy.judge(
                question.getType(), question.getAnswer(), userAnswer, question.getScore());

        answer.setUserAnswer(userAnswer);
        answer.setIsCorrect(judged.correct() ? 1 : 0);
        answer.setScore(judged.score());
        answer.setAnsweredAt(LocalDateTime.now());
        answerMapper.updateById(answer);

        updateStat(userId, question.getId(), judged.correct());

        List<QuizAnswer> all = loadAnswers(sessionId);
        int obtainedScore = all.stream().mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).sum();
        long answeredCount = all.stream().filter(a -> StringUtils.hasText(a.getUserAnswer())).count();
        session.setObtainedScore(obtainedScore);
        session.setCurrentIndex((int) answeredCount);
        sessionMapper.updateById(session);

        return new AnswerResultVO(question.getId(), judged.correct(),
                AnswerUtil.split(question.getAnswer()), question.getAnalysis(),
                judged.score(), obtainedScore, (int) answeredCount, session.getQuestionCount());
    }

    @Transactional(rollbackFor = Exception.class)
    public SessionResultVO finish(Long sessionId, Long userId) {
        QuizSession session = requireSession(sessionId, userId);
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            List<QuizAnswer> all = loadAnswers(sessionId);
            session.setObtainedScore(all.stream().mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).sum());
            session.setStatus(SessionStatus.COMPLETED);
            session.setFinishTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
        return result(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void abandon(Long sessionId, Long userId) {
        QuizSession session = requireSession(sessionId, userId);
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            session.setStatus(SessionStatus.ABANDONED);
            session.setFinishTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    public SessionResultVO result(Long sessionId, Long userId) {
        return result(requireSession(sessionId, userId));
    }

    public PageResult<SessionVO> list(long pageNo, long pageSize, String status, Long userId) {
        LambdaQueryWrapper<QuizSession> wrapper = new LambdaQueryWrapper<QuizSession>()
                .eq(QuizSession::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(QuizSession::getStatus, SessionStatus.valueOf(status.trim().toUpperCase()));
        }
        wrapper.orderByDesc(QuizSession::getId);
        IPage<QuizSession> mpPage = sessionMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<SessionVO> items = mpPage.getRecords().stream().map(this::toSessionVO).toList();
        return PageResult.of(items, mpPage.getTotal(), pageNo, pageSize);
    }

    private SessionResultVO result(QuizSession session) {
        List<QuizAnswer> answers = loadAnswers(session.getId());
        Map<Long, Question> questionMap = loadQuestionMap(answers.stream().map(QuizAnswer::getQuestionId).toList());
        List<QuizResultItemVO> items = new ArrayList<>();
        int totalScore = 0;
        int obtainedScore = 0;
        int answeredCount = 0;
        int correctCount = 0;
        for (QuizAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            boolean answered = StringUtils.hasText(answer.getUserAnswer());
            int score = question.getScore() == null ? 1 : question.getScore();
            totalScore += score;
            if (answered) {
                answeredCount++;
                obtainedScore += answer.getScore() == null ? 0 : answer.getScore();
                if (answer.getIsCorrect() != null && answer.getIsCorrect() == 1) {
                    correctCount++;
                }
            }
            items.add(new QuizResultItemVO(
                    question.getId(), answer.getSort(), question.getType(), question.getStem(), score,
                    loadOptions(question.getId()),
                    answered ? AnswerUtil.split(answer.getUserAnswer()) : List.of(),
                    AnswerUtil.split(question.getAnswer()),
                    answered,
                    answer.getIsCorrect() != null && answer.getIsCorrect() == 1,
                    question.getAnalysis()));
        }
        double accuracy = answeredCount == 0 ? 0 : (correctCount * 100.0) / answeredCount;
        return new SessionResultVO(session.getId(), session.getTitle(), session.getSourceType(),
                session.getStatus(), session.getQuestionCount(), answeredCount, correctCount,
                session.getQuestionCount() - answeredCount, totalScore, obtainedScore, accuracy,
                session.getStartTime(), session.getFinishTime(), items);
    }

    private void updateStat(Long userId, Long questionId, boolean correct) {
        QuestionStat stat = statMapper.selectOne(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getQuestionId, questionId));
        if (stat == null) {
            stat = new QuestionStat();
            stat.setUserId(userId);
            stat.setQuestionId(questionId);
            stat.setAnswerCount(0);
            stat.setCorrectCount(0);
            stat.setConsecutiveCorrect(0);
            stat.setInWrongBook(0);
        }
        stat.setAnswerCount(stat.getAnswerCount() + 1);
        if (correct) {
            stat.setCorrectCount(stat.getCorrectCount() + 1);
            stat.setConsecutiveCorrect(stat.getConsecutiveCorrect() + 1);
            if (stat.getConsecutiveCorrect() >= CONSECUTIVE_TO_LEAVE_WRONG_BOOK) {
                stat.setInWrongBook(0);
            }
        } else {
            stat.setConsecutiveCorrect(0);
            stat.setInWrongBook(1);
        }
        stat.setLastCorrect(correct ? 1 : 0);
        stat.setLastAnswerTime(LocalDateTime.now());
        if (stat.getId() == null) {
            statMapper.insert(stat);
        } else {
            statMapper.updateById(stat);
        }
    }

    private List<Long> selectWrongBookQuestionIds(Long userId, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<QuestionStat> wrapper = new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getInWrongBook, 1)
                .orderByDesc(QuestionStat::getLastAnswerTime)
                .last("LIMIT " + Math.min(count, 200));
        return statMapper.selectList(wrapper).stream().map(QuestionStat::getQuestionId).toList();
    }

    private QuizSession requireSession(Long sessionId, Long userId) {
        QuizSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "答题记录不存在");
        }
        return session;
    }

    private List<QuizAnswer> loadAnswers(Long sessionId) {
        return answerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getSessionId, sessionId)
                .orderByAsc(QuizAnswer::getSort));
    }

    private Map<Long, Question> loadQuestionMap(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Question> map = new HashMap<>();
        for (Question question : questionMapper.selectBatchIds(questionIds)) {
            map.put(question.getId(), question);
        }
        return map;
    }

    private List<OptionVO> loadOptions(Long questionId) {
        return optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, questionId)
                        .orderByAsc(QuestionOption::getSort))
                .stream()
                .map(o -> new OptionVO(o.getLabel(), o.getContent()))
                .toList();
    }

    private SessionVO toSessionVO(QuizSession session) {
        SessionVO vo = new SessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setSourceType(session.getSourceType());
        vo.setStatus(session.getStatus());
        vo.setPaperId(session.getPaperId());
        vo.setQuestionCount(session.getQuestionCount() == null ? 0 : session.getQuestionCount());
        vo.setCurrentIndex(session.getCurrentIndex() == null ? 0 : session.getCurrentIndex());
        vo.setTotalScore(session.getTotalScore() == null ? 0 : session.getTotalScore());
        vo.setObtainedScore(session.getObtainedScore() == null ? 0 : session.getObtainedScore());
        vo.setStartTime(session.getStartTime());
        vo.setFinishTime(session.getFinishTime());
        return vo;
    }
}
