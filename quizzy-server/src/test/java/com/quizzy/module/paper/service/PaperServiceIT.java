package com.quizzy.module.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.paper.dto.PaperRuleDTO;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.vo.PaperAppendResultVO;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.paper.entity.PaperQuestion;
import com.quizzy.module.paper.mapper.PaperQuestionMapper;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.module.question.service.QuestionService;
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

    @Autowired
    QuestionMapper questionMapper;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    QuestionService questionService;

    /** 保存一道题并放进指定**名字**的分类（分类随题目诞生）。 */
    private long createQuestionIn(String token, String categoryName) throws Exception {
        JsonNode res = apiPost("/api/questions", token, payload(
                "type", "SINGLE",
                "stem", "IT 分类题 " + newUsername(),
                "score", 1,
                "categoryName", categoryName,
                "answers", List.of("A"),
                "options", List.of(
                        payload("label", "A", "content", "A"),
                        payload("label", "B", "content", "B"))));
        assertThat(res.path("code").asInt()).as("造题失败：%s", res).isZero();
        return res.path("data").asLong();
    }

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
    @DisplayName("规则卷引用的分类已不存在：那条条件失效，而不是整张卷抽不到题")
    void ruleWithMissingCategoryStillDraws() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long userId = me.path("user").path("id").asLong();

        // 先造一个「已经消失的分类 id」：分类随题目诞生，把那道题删掉它就被回收了
        long questionId = createQuestionIn(token, "IT 将消失 " + newUsername());
        long deadCategoryId = questionMapper.selectById(questionId).getCategoryId();
        questionService.delete(questionId, userId);
        assertThat(categoryMapper.selectById(deadCategoryId)).as("分类该已被回收").isNull();

        PaperRuleDTO rule = new PaperRuleDTO();
        rule.setCategoryId(deadCategoryId);
        rule.setTypes(List.of(QuestionType.SINGLE));
        rule.setCount(1);

        // 「失效」= 跳过分类筛选、其余条件照常，所以仍然抽得到题。
        // ⚠️ 少了那道判断，这里会是 0 题——用户只看到「没有符合要求的题目」却不知原因。
        assertThat(paperService.preview(rule, userId)).isNotEmpty();
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

    // ---------- 空卷与追加题目（ADR 0026） ----------

    @Test
    @DisplayName("空固定卷也能存：questionCount 为 0、关系表为空")
    void emptyFixedPaperIsAllowed() throws Exception {
        JsonNode me = newAccount();
        long owner = me.path("user").path("id").asLong();

        Long paperId = paperService.save(fixedPaper("IT 空卷 " + newUsername(), List.of()), owner);

        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isZero();
        assertThat(paperService.detail(paperId, owner).getQuestionIds()).isEmpty();
        assertThat(storedRelations(paperId)).isEmpty();
    }

    @Test
    @DisplayName("空固定卷：questionIds 传 null 也不炸（前端没选题就是这个形状）")
    void fixedPaperWithNullQuestionIdsIsAllowed() throws Exception {
        JsonNode me = newAccount();
        long owner = me.path("user").path("id").asLong();

        Long paperId = paperService.save(fixedPaper("IT 空卷null " + newUsername(), null), owner);

        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isZero();
        assertThat(storedRelations(paperId)).isEmpty();
    }

    @Test
    @DisplayName("固定卷入参去重：同一个 id 传两次不会炸在唯一键上")
    void duplicateQuestionIdsAreDeduped() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();
        long q1 = createQuestion(token);

        Long paperId = paperService.save(fixedPaper("IT 去重卷 " + newUsername(), List.of(q1, q1)), owner);

        assertThat(storedRelations(paperId)).containsExactly(q1);
        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("追加题目：并入已有列表，新题接在末尾")
    void appendQuestionsMergesIntoPaper() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();
        long q1 = createQuestion(token);
        long q2 = createQuestion(token);
        long q3 = createQuestion(token);

        Long paperId = paperService.save(fixedPaper("IT 追加卷 " + newUsername(), List.of(q1)), owner);

        PaperAppendResultVO result = paperService.appendQuestions(paperId, List.of(q2, q3), owner);

        assertThat(result.added()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(3);
        // 顺序也要对：q1 还在最前，新加的两道按入参顺序跟在后面
        assertThat(storedRelations(paperId)).containsExactly(q1, q2, q3);
        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("追加题目：已在卷里的题被忽略，重复调用不会越加越多")
    void appendQuestionsIgnoresAlreadyPresent() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();
        long q1 = createQuestion(token);
        long q2 = createQuestion(token);

        Long paperId = paperService.save(fixedPaper("IT 重复追加卷 " + newUsername(), List.of(q1)), owner);

        PaperAppendResultVO first = paperService.appendQuestions(paperId, List.of(q1, q2), owner);
        assertThat(first.added()).as("q1 已在卷里，只该新增 q2").isEqualTo(1);
        assertThat(first.total()).isEqualTo(2);

        PaperAppendResultVO second = paperService.appendQuestions(paperId, List.of(q1, q2), owner);
        assertThat(second.added()).as("再来一次应当一道都加不进去").isZero();
        assertThat(second.total()).isEqualTo(2);
        assertThat(storedRelations(paperId)).containsExactly(q1, q2);
    }

    @Test
    @DisplayName("追加题目：规则卷不行——它根本没有题目列表")
    void appendQuestionsRejectsRulePaper() throws Exception {
        JsonNode me = newAccount();
        long owner = me.path("user").path("id").asLong();

        PaperRuleDTO rule = new PaperRuleDTO();
        rule.setTypes(List.of(QuestionType.SINGLE));
        rule.setCount(5);
        PaperSaveDTO rulePaper = new PaperSaveDTO();
        rulePaper.setTitle("IT 规则卷 " + newUsername());
        rulePaper.setMode(PaperMode.RULE);
        rulePaper.setRule(rule);
        Long paperId = paperService.save(rulePaper, owner);

        assertThatThrownBy(() -> paperService.appendQuestions(paperId, List.of(1L), owner))
                .hasMessageContaining("固定卷");
    }

    @Test
    @DisplayName("追加题目到别人的卷：报 404，且真的没加进去")
    void appendQuestionsRejectsOtherUsersPaper() throws Exception {
        JsonNode owner = newAccount();
        JsonNode other = newAccount();
        long ownerId = owner.path("user").path("id").asLong();
        long otherId = other.path("user").path("id").asLong();

        Long paperId = paperService.save(fixedPaper("IT 别人的追加卷 " + newUsername(), List.of()), ownerId);
        long myQuestion = createQuestion(other.path("token").asText());

        assertThatThrownBy(() -> paperService.appendQuestions(paperId, List.of(myQuestion), otherId))
                .hasMessageContaining("不存在");
        assertThat(storedRelations(paperId)).isEmpty();
        assertThat(paperService.detail(paperId, ownerId).getQuestionCount()).isZero();
    }

    @Test
    @DisplayName("追加题目：看不见的题（别人的私有题）不许加进来——否则能借作答把题干读出来")
    void appendQuestionsRejectsInvisibleQuestion() throws Exception {
        JsonNode a = newAccount();
        JsonNode b = newAccount();
        long bId = b.path("user").path("id").asLong();

        long privateQuestionOfA = createQuestion(a.path("token").asText());
        Long paperOfB = paperService.save(fixedPaper("IT 越权加题 " + newUsername(), List.of()), bId);

        assertThatThrownBy(() -> paperService.appendQuestions(paperOfB, List.of(privateQuestionOfA), bId))
                .as("id 是自增的、猜得到，所以必须按可见性拒绝")
                .hasMessageContaining("不属于你");
        assertThat(storedRelations(paperOfB)).isEmpty();
    }

    @Test
    @DisplayName("存卷时不许塞进别人的私有题——id 是自增的，塞进来就能借作答读到题干")
    void saveRejectsOtherUsersPrivateQuestion() throws Exception {
        JsonNode a = newAccount();
        JsonNode b = newAccount();
        long bId = b.path("user").path("id").asLong();
        long privateQuestionOfA = createQuestion(a.path("token").asText());

        assertThatThrownBy(() -> paperService.save(
                fixedPaper("IT 越权存卷 " + newUsername(), List.of(privateQuestionOfA)), bId))
                .hasMessageContaining("不属于你");
    }

    @Test
    @DisplayName("改卷时，卷里那道已被自己软删的题允许继续挂着——不能因为它就整张卷保存不了")
    void updatingPaperKeepsItsOwnDeletedQuestion() throws Exception {
        JsonNode me = newAccount();
        String token = me.path("token").asText();
        long owner = me.path("user").path("id").asLong();

        long kept = createQuestion(token);
        long deleted = createQuestion(token);
        Long paperId = paperService.save(fixedPaper("IT 含删题卷 " + newUsername(), List.of(kept, deleted)), owner);

        // 把卷里的一道题删掉，再回来改这张卷——它仍引用着那道题
        questionService.delete(deleted, owner);

        PaperSaveDTO update = fixedPaper("IT 含删题卷改名 " + newUsername(), List.of(kept, deleted));
        update.setId(paperId);
        paperService.save(update, owner);

        assertThat(storedRelations(paperId)).containsExactly(kept, deleted);
        assertThat(paperService.detail(paperId, owner).getQuestionCount()).isEqualTo(2);
    }
}
