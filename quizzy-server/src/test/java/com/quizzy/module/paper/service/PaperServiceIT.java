package com.quizzy.module.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.entity.PaperQuestion;
import com.quizzy.module.paper.mapper.PaperQuestionMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 试卷的**服务集成**测试：直接调 service，并核对**跨表状态**。
 *
 * <p>与接口层的分工：接口层（{@code PaperApiIT}）验的是 HTTP 契约与越权；
 * 这里验的是「一次 save 到底在几张表里留下了什么」——
 * 只断言 {@code PaperVO} 是不够的，VO 可以由内存里的入参拼出来，
 * 而 {@code paper_question} 关系表是不是真的一致，只有查表才知道。
 */
@DisplayName("服务集成 · 试卷（跨表状态）")
class PaperServiceIT extends ApiTestBase {

    @Autowired
    PaperService paperService;

    @Autowired
    PaperQuestionMapper paperQuestionMapper;

    private long createQuestion(String token) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 卷用题 " + newUsername(),
                "score", 2,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("造题失败：%s", res).isZero();
        return res.path("data").asLong();
    }

    /** 直接读关系表——这才是「两表一致」的证据，VO 不算。 */
    private List<Long> storedRelations(long paperId) {
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, paperId)
                        .orderByAsc(PaperQuestion::getSort))
                .stream()
                .map(PaperQuestion::getQuestionId)
                .toList();
    }

    private static PaperSaveDTO fixedPaper(String title, List<Long> questionIds) {
        PaperSaveDTO dto = new PaperSaveDTO();
        dto.setTitle(title);
        dto.setMode(PaperMode.FIXED);
        dto.setQuestionIds(questionIds);
        return dto;
    }

    @Test
    @DisplayName("存固定卷：paper 与 paper_question 两表一致")
    void fixedPaperPersistsRelations() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();

        long q1 = createQuestion(token);
        long q2 = createQuestion(token);

        Long paperId = paperService.save(fixedPaper("IT 固定卷 " + newUsername(), List.of(q1, q2)), owner);

        assertThat(paperService.detail(paperId, owner).getQuestionIds()).containsExactly(q1, q2);
        // 关键：关系表也要有两条，且顺序与入参一致
        assertThat(storedRelations(paperId)).containsExactly(q1, q2);
    }

    @Test
    @DisplayName("改卷换题：旧关系被替换，不残留")
    void updatingPaperReplacesRelations() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();

        long q1 = createQuestion(token);
        long q2 = createQuestion(token);
        long q3 = createQuestion(token);

        Long paperId = paperService.save(fixedPaper("IT 待改卷 " + newUsername(), List.of(q1, q2)), owner);

        PaperSaveDTO update = fixedPaper("IT 改过的卷 " + newUsername(), List.of(q3));
        update.setId(paperId);
        paperService.save(update, owner);

        // 旧的两条关系必须没了——残留会让「固定卷」变成「越改题越多」
        assertThat(storedRelations(paperId)).containsExactly(q3);
        assertThat(paperService.detail(paperId, owner).getQuestionIds()).containsExactly(q3);
        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("别人的卷：改不动也看不到，报 404")
    void otherUsersPaperIsInvisible() throws Exception {
        JsonNode owner = newAccount();
        JsonNode other = newAccount();
        long ownerId = owner.path("user").path("id").asLong();

        Long paperId = paperService.save(
                fixedPaper("IT 别人的卷 " + newUsername(), List.of(createQuestion(owner.path("token").asText()))),
                ownerId);

        long otherId = other.path("user").path("id").asLong();
        assertThatThrownBy(() -> paperService.detail(paperId, otherId))
                .as("别人的卷应当连存在性都不确认（防探测）")
                .hasMessageContaining("不存在");
    }
}
