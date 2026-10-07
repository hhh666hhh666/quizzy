package com.quizzy.module.auth.service;

import com.quizzy.module.auth.mapper.AccountCleanupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注销账号的数据清理。取舍与「为什么是硬删」见 docs/adr/0029。
 *
 * <p>整段必须是**一个事务**：库里没有任何外键，中途失败会留下谁也认领不了的半截数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final AccountCleanupMapper cleanupMapper;

    @Transactional
    public void deleteEverythingOf(Long userId) {
        int answers = cleanupMapper.deleteAnswers(userId);
        int sessions = cleanupMapper.deleteSessions(userId);
        int stats = cleanupMapper.deleteStats(userId);
        int paperQuestions = cleanupMapper.deletePaperQuestions(userId);
        int papers = cleanupMapper.deletePapers(userId);
        int tags = cleanupMapper.deleteQuestionTags(userId);
        int options = cleanupMapper.deleteQuestionOptions(userId);
        int questionStats = cleanupMapper.deleteStatsOfOwnedQuestions(userId);
        int questions = cleanupMapper.deleteQuestions(userId);
        int user = cleanupMapper.deleteUser(userId);

        // 只记条数、不记内容：注销是用户的隐私动作，日志里不该留下他做过什么。
        log.info("账号已注销 userId={}：作答 {} / 会话 {} / 统计 {} / 试卷题 {} / 试卷 {} / 标签关联 {} / 选项 {} / 题目统计 {} / 题目 {} / 账号 {}",
                userId, answers, sessions, stats, paperQuestions, papers, tags, options, questionStats, questions, user);
    }
}
