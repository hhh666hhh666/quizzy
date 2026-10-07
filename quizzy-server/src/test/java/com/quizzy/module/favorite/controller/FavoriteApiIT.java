package com.quizzy.module.favorite.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.mapper.QuestionMapper;
import com.quizzy.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 收藏夹的接口测试。模型见 docs/adr/0030，这里钉住的正是那张卡上几条最容易做错的约定：
 *
 * <ul>
 *   <li>「收藏」= 在至少一个夹里 → 取消收藏要**从所有夹**移出；
 *   <li>删夹只把题移出，题若因此不属于任何夹就**不再是收藏**；
 *   <li>「修改收藏夹」是**覆盖**语义，**空选 = 留在默认夹**（不是取消收藏）；
 *   <li>题库页批量「加入」是**只加不减**（两个入口名字不同、行为也不同）；
 *   <li>默认收藏夹**按需创建**、**不可删**、**可改名**。
 * </ul>
 */
@DisplayName("接口 · 收藏夹")
class FavoriteApiIT extends ApiTestBase {

    @Autowired
    private QuestionMapper questionMapper;

    // ---------- 助手 ----------

    /**
     * 关键词筛选走 like，所以题干必须是 ASCII 且唯一——中文进 query 还要额外编码，不值当。
     */
    private static String asciiStem(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static long idOf(JsonNode account) {
        return account.path("user").path("id").asLong();
    }

    private static String tokenOf(JsonNode account) {
        return account.path("token").asText();
    }

    /** 造一道属于某人的私有题。收藏相关的用例都要求题对操作者可见，所以归属不能随便写。 */
    private long newQuestion(long ownerId, String stem) {
        Question question = new Question();
        question.setType(QuestionType.SINGLE);
        question.setStem(stem);
        question.setAnswer("A");
        question.setScore(1);
        question.setOwnerId(ownerId);
        questionMapper.insert(question);
        return question.getId();
    }

    private String stemOf(long questionId) {
        Question question = questionMapper.selectById(questionId);
        return question == null ? "" : question.getStem();
    }

    private JsonNode folders(String token) throws Exception {
        JsonNode res = apiGet("/api/favorites/folders", token);
        assertThat(res.path("code").asInt()).as("收藏夹列表应当成功，实际响应：%s", res).isZero();
        return res.path("data");
    }

    private Long defaultFolderId(String token) throws Exception {
        for (JsonNode folder : folders(token)) {
            if (folder.path("isDefault").asBoolean()) {
                return folder.path("id").asLong();
            }
        }
        return null;
    }

    private String defaultFolderName(String token) throws Exception {
        for (JsonNode folder : folders(token)) {
            if (folder.path("isDefault").asBoolean()) {
                return folder.path("name").asText();
            }
        }
        return null;
    }

    private long createFolder(String token, String name) throws Exception {
        JsonNode res = apiPost("/api/favorites/folders", token, payload("name", name));
        assertThat(res.path("code").asInt()).as("建夹应当成功，实际响应：%s", res).isZero();
        return res.path("data").path("id").asLong();
    }

    /** 收藏一道题，顺带把默认夹创建出来（默认夹是按需创建的，见 ADR 0030）。 */
    private long favorite(String token, long questionId) throws Exception {
        JsonNode res = apiPost("/api/favorites/questions/" + questionId, token, null);
        assertThat(res.path("code").asInt()).as("收藏应当成功，实际响应：%s", res).isZero();
        return res.path("data").path("id").asLong();
    }

    private List<Long> folderIdsOf(String token, long questionId) throws Exception {
        JsonNode res = apiGet("/api/favorites/questions/" + questionId + "/folders", token);
        assertThat(res.path("code").asInt()).isZero();
        List<Long> ids = new ArrayList<>();
        res.path("data").forEach(node -> ids.add(node.asLong()));
        return ids;
    }

    private void setFolders(String token, long questionId, List<Long> folderIds) throws Exception {
        JsonNode res = apiPut("/api/favorites/questions/" + questionId + "/folders", token,
                payload("folderIds", folderIds));
        assertThat(res.path("code").asInt()).as("设置所属夹应当成功，实际响应：%s", res).isZero();
    }

    private long totalOf(String token, String queryString) throws Exception {
        return apiGet("/api/questions?" + queryString, token).path("data").path("total").asLong();
    }

    private String nameOfFolder(String token, long folderId) throws Exception {
        for (JsonNode folder : folders(token)) {
            if (folder.path("id").asLong() == folderId) {
                return folder.path("name").asText();
            }
        }
        return null;
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("默认收藏夹按需创建：新账号一个夹都没有，一收藏就出现，统计与标记跟着变")
    void defaultFolderIsCreatedOnDemand() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);

        assertThat(folders(token).size()).as("新账号一开始没有任何收藏夹").isZero();

        String stem = asciiStem("fav");
        long questionId = newQuestion(userId, stem);

        JsonNode favorited = apiPost("/api/favorites/questions/" + questionId, token, null);
        assertThat(favorited.path("code").asInt()).as("收藏应当成功，实际响应：%s", favorited).isZero();
        assertThat(favorited.path("data").path("isDefault").asBoolean()).as("星标收藏落在默认夹").isTrue();
        String defaultName = favorited.path("data").path("name").asText();
        assertThat(defaultName).isNotEmpty();

        JsonNode list = folders(token);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).path("name").asText()).isEqualTo(defaultName);
        assertThat(list.get(0).path("questionCount").asLong()).isEqualTo(1);
        // 排序靠它，空夹会是 null——有题进来就该有值
        assertThat(list.get(0).hasNonNull("lastAddedTime")).isTrue();

        assertThat(totalOf(token, "keyword=" + stem)).isEqualTo(1);
        assertThat(apiGet("/api/questions?keyword=" + stem, token)
                .path("data").path("list").get(0).path("favorited").asBoolean()).isTrue();
        assertThat(apiGet("/api/questions/" + questionId, token).path("data").path("favorited").asBoolean())
                .isTrue();
    }

    @Test
    @DisplayName("收藏夹重名返回 409；默认夹可以改名，但改成的名字也不能和别人撞")
    void duplicateFolderNameIsRejected() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long questionId = newQuestion(userId, asciiStem("ren"));

        createFolder(token, "重点");
        JsonNode again = apiPost("/api/favorites/folders", token, payload("name", "重点"));
        assertThat(again.path("code").asInt()).isEqualTo(409);
        assertThat(again.hasNonNull("data")).isFalse();

        // 收藏一下，让默认夹诞生
        long defaultId = favorite(token, questionId);
        assertThat(defaultFolderId(token)).isEqualTo(defaultId);

        assertThat(apiPut("/api/favorites/folders/" + defaultId, token, payload("name", "重点"))
                .path("code").asInt()).as("默认夹改名也不能撞上已有名字").isEqualTo(409);
        assertThat(apiPut("/api/favorites/folders/" + defaultId, token, payload("name", "我的收藏"))
                .path("code").asInt()).isZero();
        assertThat(nameOfFolder(token, defaultId)).isEqualTo("我的收藏");
        assertThat(defaultFolderId(token)).as("改名不该丢掉默认标记").isEqualTo(defaultId);
    }

    @Test
    @DisplayName("修改收藏夹是覆盖语义：勾谁属于谁；一个都不勾 = 留在默认收藏夹（不是取消收藏）")
    void setFoldersOverwritesAndEmptyMeansDefault() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        String stem = asciiStem("ovr");
        long questionId = newQuestion(userId, stem);

        long defaultId = favorite(token, questionId);
        assertThat(folderIdsOf(token, questionId)).containsExactly(defaultId);

        long a = createFolder(token, "A");
        long b = createFolder(token, "B");

        setFolders(token, questionId, List.of(a, b));
        assertThat(folderIdsOf(token, questionId))
                .as("勾了就属于那些夹，不再属于默认夹").containsExactlyInAnyOrder(a, b);

        setFolders(token, questionId, List.of());
        assertThat(folderIdsOf(token, questionId)).as("空选 = 留在默认夹，而不是取消收藏").containsExactly(defaultId);
        assertThat(totalOf(token, "keyword=" + stem + "&anyFavorite=true")).isEqualTo(1);
    }

    @Test
    @DisplayName("取消收藏：从所有夹一起移出，并把被移出的夹返回给界面做撤销")
    void unfavoriteRemovesFromEveryFolderAndReturnsThem() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long questionId = newQuestion(userId, asciiStem("unf"));

        long a = createFolder(token, "A");
        long b = createFolder(token, "B");
        setFolders(token, questionId, List.of(a, b));

        JsonNode res = apiDelete("/api/favorites/questions/" + questionId, token);
        assertThat(res.path("code").asInt()).isZero();
        List<Long> removed = new ArrayList<>();
        res.path("data").forEach(node -> removed.add(node.asLong()));
        assertThat(removed).containsExactlyInAnyOrder(a, b);

        assertThat(folderIdsOf(token, questionId)).isEmpty();

        // 撤销：客户端把上面那批 id 原样送回来即可（不需要新接口）
        setFolders(token, questionId, removed);
        assertThat(folderIdsOf(token, questionId)).containsExactlyInAnyOrder(a, b);
    }

    @Test
    @DisplayName("删夹只把题从它里面移出；题若因此不属于任何夹就不再是收藏；默认夹不可删")
    void deleteFolderUnfavoritesOnlyWhenLastFolderGone() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long questionId = newQuestion(userId, asciiStem("del"));

        long a = createFolder(token, "A");
        long b = createFolder(token, "B");
        setFolders(token, questionId, List.of(a, b));

        assertThat(apiDelete("/api/favorites/folders/" + a, token).path("code").asInt()).isZero();
        assertThat(folderIdsOf(token, questionId)).as("另一个夹还在，题就还是收藏").containsExactly(b);

        assertThat(apiDelete("/api/favorites/folders/" + b, token).path("code").asInt()).isZero();
        assertThat(folderIdsOf(token, questionId)).as("不属于任何夹了，就不再是收藏").isEmpty();

        long defaultId = favorite(token, questionId);
        assertThat(apiDelete("/api/favorites/folders/" + defaultId, token).path("code").asInt())
                .as("默认夹删了，随手收藏就无处可落").isEqualTo(400);
        assertThat(defaultFolderId(token)).isNotNull();
    }

    @Test
    @DisplayName("批量加入是只加不减；从某个夹移出也不等于取消收藏")
    void batchAddIsAddOnly() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long first = newQuestion(userId, asciiStem("bat1"));
        long second = newQuestion(userId, asciiStem("bat2"));

        long a = createFolder(token, "A");
        long b = createFolder(token, "B");
        setFolders(token, first, List.of(a));

        assertThat(apiPost("/api/favorites/folders/" + b + "/questions", token,
                payload("questionIds", List.of(first, second))).path("code").asInt()).isZero();
        assertThat(folderIdsOf(token, first)).as("批量加入不该把它从 A 里挪走").containsExactlyInAnyOrder(a, b);
        assertThat(folderIdsOf(token, second)).containsExactly(b);

        assertThat(apiDelete("/api/favorites/folders/" + b + "/questions/" + first, token)
                .path("code").asInt()).isZero();
        assertThat(folderIdsOf(token, first)).containsExactly(a);
    }

    @Test
    @DisplayName("题库筛选：按夹筛与「全部收藏」都生效，两个一起给时以「全部收藏」为准")
    void favoriteFiltersWork() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        String prefix = "favq-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long inside = newQuestion(userId, prefix + "-inside");
        newQuestion(userId, prefix + "-outside");

        long a = createFolder(token, "A");
        setFolders(token, inside, List.of(a));

        assertThat(totalOf(token, "keyword=" + prefix)).isEqualTo(2);
        assertThat(totalOf(token, "keyword=" + prefix + "&anyFavorite=true")).isEqualTo(1);
        assertThat(totalOf(token, "keyword=" + prefix + "&favoriteFolderIds=" + a)).isEqualTo(1);
        assertThat(totalOf(token, "keyword=" + prefix + "&favoriteFolderIds=" + (a + 99999))).isZero();
        // 「全部收藏」与具体夹一起给 → 前者覆盖后者（包含关系），仍只命中那一道
        assertThat(totalOf(token, "keyword=" + prefix + "&anyFavorite=true&favoriteFolderIds=" + a)).isEqualTo(1);
    }

    @Test
    @DisplayName("夹列表按「最近有新题进来」倒序")
    void folderListOrderFollowsRecentAdditions() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long first = newQuestion(userId, asciiStem("ord1"));
        long second = newQuestion(userId, asciiStem("ord2"));

        long a = createFolder(token, "A");
        long b = createFolder(token, "B");
        apiPost("/api/favorites/folders/" + a + "/questions", token, payload("questionIds", List.of(first)));
        apiPost("/api/favorites/folders/" + b + "/questions", token, payload("questionIds", List.of(second)));

        JsonNode list = folders(token);
        assertThat(list.size()).as("两个夹都没被收藏动作碰过，默认夹此时还不存在").isEqualTo(2);
        assertThat(list.get(0).path("name").asText()).as("刚有新题进来的 B 排最前").isEqualTo("B");
        assertThat(list.get(1).path("name").asText()).isEqualTo("A");
    }

    @Test
    @DisplayName("用收藏开练习：来源是 FAVORITE、标题带夹名；别人的夹取不到；不传夹 = 全部收藏")
    void practiceFromFolder() throws Exception {
        JsonNode account = newAccount();
        String token = tokenOf(account);
        long userId = idOf(account);
        long questionId = newQuestion(userId, asciiStem("pra"));

        long a = createFolder(token, "A");
        setFolders(token, questionId, List.of(a));

        JsonNode started = apiPost("/api/favorites/practice?folderId=" + a + "&count=5", token, null);
        assertThat(started.path("code").asInt()).as("开练习应当成功，实际响应：%s", started).isZero();
        long sessionId = started.path("data").asLong();

        JsonNode session = apiGet("/api/quiz/sessions/" + sessionId, token).path("data");
        assertThat(session.path("sourceType").asText()).isEqualTo("FAVORITE");
        assertThat(session.path("title").asText()).contains("A");
        assertThat(session.path("questionCount").asInt()).isEqualTo(1);

        // 别人的夹：按不存在处理，也不泄漏别人夹里的题
        String otherToken = newUserToken();
        assertThat(apiPost("/api/favorites/practice?folderId=" + a, otherToken, null).path("code").asInt())
                .isEqualTo(404);

        // 不传夹 = 全部收藏
        assertThat(apiPost("/api/favorites/practice", token, null).path("code").asInt()).isZero();
    }

    @Test
    @DisplayName("别人的收藏夹不可见；别人的私有题也收藏不了；空题目列表被拒")
    void othersFoldersAndPrivateQuestionsAreOffLimits() throws Exception {
        JsonNode owner = newAccount();
        long ownerId = idOf(owner);
        String ownerToken = tokenOf(owner);
        long folderId = createFolder(ownerToken, "A");
        long privateQuestion = newQuestion(ownerId, asciiStem("pri"));

        JsonNode intruder = newAccount();
        String intruderToken = tokenOf(intruder);

        assertThat(apiPut("/api/favorites/folders/" + folderId, intruderToken, payload("name", "X"))
                .path("code").asInt()).isEqualTo(404);
        assertThat(apiDelete("/api/favorites/folders/" + folderId, intruderToken).path("code").asInt())
                .isEqualTo(404);
        assertThat(apiPost("/api/favorites/folders/" + folderId + "/questions", intruderToken,
                payload("questionIds", List.of(privateQuestion))).path("code").asInt()).isEqualTo(404);
        assertThat(apiDelete("/api/favorites/folders/" + folderId + "/questions/" + privateQuestion,
                intruderToken).path("code").asInt()).isEqualTo(404);

        // 别人的私有题也不能收藏（公开题才行）
        assertThat(apiPost("/api/favorites/questions/" + privateQuestion, intruderToken, null)
                .path("code").asInt()).isEqualTo(404);
        // 空列表被参数校验拦下
        assertThat(apiPost("/api/favorites/folders/" + folderId + "/questions", ownerToken,
                payload("questionIds", List.of())).path("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("收藏相关接口都不裸奔：不带 token 一律信封 401")
    void favoriteEndpointsRequireLogin() throws Exception {
        long questionId = newQuestion(1L, asciiStem("anon"));
        assertThat(apiGet("/api/favorites/folders", null).path("code").asInt()).isEqualTo(401);
        assertThat(apiPost("/api/favorites/questions/" + questionId, null, null).path("code").asInt())
                .isEqualTo(401);
        assertThat(apiPost("/api/favorites/folders", null, payload("name", "x")).path("code").asInt())
                .isEqualTo(401);
        assertThat(apiPost("/api/favorites/practice", null, null).path("code").asInt()).isEqualTo(401);
    }
}
