package com.quizzy.module.wrongbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.quiz.dto.AnswerDTO;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.mapper.QuizSessionMapper;
import com.quizzy.module.quiz.service.QuizService;
import com.quizzy.module.quiz.vo.SessionVO;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错题本的**服务集成**测试。
 *
 * <p>错题本**不是一张独立的表**，而是 {@code question_stat} 投影出来的状态。
 * 所以这里的每条断言都在验「投影」本身：
 *
 * <ul>
 *   <li>答错 → 它出现；
 *   <li>手动移出 → {@code inWrongBook} 翻回 0（**不是删掉统计行**）；
 *   <li>错题重练 → 抽到的就是本里的题。
 * </ul>
 */
@DisplayName("服务集成 · 错题本（投影状态）")
class WrongBookServiceIT extends ApiTestBase {

    @Autowired
    WrongBookService wrongBookService;

    @Autowired
    QuizService quizService;

    @Autowired
    QuestionStatMapper statMapper;

    @Autowired
    QuizSessionMapper sessionMapper;

    /** 答错一道题，返回它的 id。 */
    private long answerOneWrong(long userId) {
        PaperRuleDTO rule = new PaperRuleDTO();
        rule.setTypes(List.of(QuestionType.SINGLE));
        rule.setCount(1);

        QuizStartDTO dto = new QuizStartDTO();
        dto.setSourceType(SourceType.QUICK);
        dto.setRule(rule);
        dto.setCount(1);

        long sessionId = quizService.start(dto, userId);
        SessionVO detail = quizService.detail(sessionId, userId);
        long questionId = detail.getQuestions().get(0).getQuestionId();

        AnswerDTO answer = new AnswerDTO();
        answer.setQuestionId(questionId);
        answer.setAnswer("Z");
        quizService.answer(sessionId, answer, userId);
        return questionId;
    }

    private List<Long> wrongBookIds(long userId) {
        return wrongBookService.page(1, 100, userId).list()
                .stream()
                .map(QuestionListItemVO::getId)
                .toList();
    }

    @Test
    @DisplayName("答错 → 出现在错题本里")
    void wrongAnswerEntersWrongBook() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long questionId = answerOneWrong(userId);

        assertThat(wrongBookIds(userId)).contains(questionId);
    }

    @Test
    @DisplayName("手动移出：本里消失，但统计行还在（只是标记翻回 0）")
    void removeOnlyFlipsTheFlag() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long questionId = answerOneWrong(userId);
        assertThat(wrongBookIds(userId)).contains(questionId);

        wrongBookService.remove(questionId, userId);

        assertThat(wrongBookIds(userId)).doesNotContain(questionId);
        // 关键：移出**不是删统计**——作答次数与正确率都还在，只是不再属于错题本
        QuestionStat stat = statMapper.selectOne(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getQuestionId, questionId));
        assertThat(stat).as("统计行应当还在").isNotNull();
        assertThat(stat.getInWrongBook()).isZero();
        assertThat(stat.getAnswerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("错题重练：抽到的就是错题本里的题")
    void practiceDrawsFromWrongBook() throws Exception {
        JsonNode me = newAccount();
        long userId = me.path("user").path("id").asLong();

        long questionId = answerOneWrong(userId);

        long sessionId = wrongBookService.practice(1, userId);
        SessionVO detail = quizService.detail(sessionId, userId);

        assertThat(detail.getQuestions()).hasSize(1);
        assertThat(detail.getQuestions().get(0).getQuestionId()).isEqualTo(questionId);
    }

    @Test
    @DisplayName("错题本是每人一份：别人的错题不会出现在我的本里")
    void wrongBookIsPerUser() throws Exception {
        JsonNode me = newAccount();
        JsonNode other = newAccount();
        long mine = me.path("user").path("id").asLong();
        long theirs = other.path("user").path("id").asLong();

        long questionId = answerOneWrong(mine);

        assertThat(wrongBookIds(mine)).contains(questionId);
        // 公开题谁都能答，但错题本是**各自**的——别人的本里不该有它
        assertThat(wrongBookIds(theirs)).doesNotContain(questionId);
        assertThat(wrongBookService.page(1, 100, theirs).total()).isZero();
    }
}
