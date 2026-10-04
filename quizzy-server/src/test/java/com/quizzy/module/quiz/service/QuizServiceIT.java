package com.quizzy.module.quiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.common.BusinessException;
import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.quiz.dto.AnswerDTO;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.entity.QuizAnswer;
import com.quizzy.module.quiz.entity.QuizSession;
import com.quizzy.module.quiz.enums.SessionStatus;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.mapper.QuizAnswerMapper;
import com.quizzy.module.quiz.mapper.QuizSessionMapper;
import com.quizzy.module.quiz.vo.SessionVO;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 答题会话的**服务集成**测试：一次作答到底在几张表里留下了什么。
 *
 * <p>接口层（{@code QuizApiIT}）验的是 HTTP 契约与越权；这里验的是跨表后果——
 * 其中最该盯的一条是「判分 → {@code question_stat} → 错题本」这条链：
 * **错题本不是一个独立的表，而是统计表投影出来的状态**，所以判错一次就该让它出现。
 */
@DisplayName("服务集成 · 答题会话（跨表状态）")
class QuizServiceIT extends ApiTestBase {

    @Autowired
    QuizService quizService;

    @Autowired
    QuizSessionMapper sessionMapper;

    @Autowired
    QuizAnswerMapper answerMapper;

    @Autowired
    QuestionStatMapper statMapper;

    /** 发起一次只含一道题的快速练习。 */
    private long startOneQuestionQuiz(long userId) {
        PaperRuleDTO rule = new PaperRuleDTO();
        rule.setTypes(List.of(QuestionType.SINGLE));
        rule.setCount(1);

        QuizStartDTO dto = new QuizStartDTO();
        dto.setSourceType(SourceType.QUICK);
        dto.setRule(rule);
        dto.setCount(1);
        return quizService.start(dto, userId);
    }

    private QuestionStat statOf(long userId, long questionId) {
        return statMapper.selectOne(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getQuestionId, questionId));
    }

    @Test
    @DisplayName("发起会话：quiz_session 与 quiz_answer 两表一致")
    void startCreatesSessionAndAnswers() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long sessionId = startOneQuestionQuiz(userId);

        QuizSession session = sessionMapper.selectById(sessionId);
        assertThat(session.getQuestionCount()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        // 关系表里必须真的有一行——只有它才证明「题目被装进了这次答题」
        assertThat(answerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getSessionId, sessionId))).isEqualTo(1);
    }

    @Test
    @DisplayName("判错：统计落库，且错题本标记被点亮（错题本是统计的投影）")
    void wrongAnswerLightsUpWrongBookFlag() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long sessionId = startOneQuestionQuiz(userId);
        SessionVO detail = quizService.detail(sessionId, userId);
        long questionId = detail.getQuestions().get(0).getQuestionId();

        // 提交一个不存在的选项，必定判错——不需要事先知道正确答案
        AnswerDTO answer = new AnswerDTO();
        answer.setQuestionId(questionId);
        answer.setAnswer("Z");
        quizService.answer(sessionId, answer, userId);

        // 统计表：答过一次、没答对、进了错题本
        QuestionStat stat = statOf(userId, questionId);
        assertThat(stat).as("判分必须落进 question_stat").isNotNull();
        assertThat(stat.getAnswerCount()).isEqualTo(1);
        assertThat(stat.getCorrectCount()).isZero();
        assertThat(stat.getInWrongBook()).as("答错应当点亮错题本标记（投影）").isEqualTo(1);
    }

    @Test
    @DisplayName("结算：会话进终态并落下结束时间")
    void finishMarksSessionCompleted() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long sessionId = startOneQuestionQuiz(userId);
        quizService.finish(sessionId, userId);

        QuizSession session = sessionMapper.selectById(sessionId);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getFinishTime()).as("结算应当落下结束时间").isNotNull();
    }

    @Test
    @DisplayName("别人的会话：连存在性都不确认（防探测）")
    void otherUsersSessionIsInvisible() throws Exception {
        JsonNode owner = newAccount();
        JsonNode other = newAccount();
        long sessionId = startOneQuestionQuiz(owner.path("user").path("id").asLong());

        assertThatThrownBy(() -> quizService.detail(sessionId, other.path("user").path("id").asLong()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }
}
