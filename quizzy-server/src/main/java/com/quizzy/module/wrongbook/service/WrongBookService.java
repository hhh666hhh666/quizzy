package com.quizzy.module.wrongbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.common.PageResult;
import com.quizzy.module.question.dto.QuestionQueryDTO;
import com.quizzy.module.question.entity.QuestionStat;
import com.quizzy.module.question.mapper.QuestionStatMapper;
import com.quizzy.module.question.service.QuestionService;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.quiz.dto.QuizStartDTO;
import com.quizzy.module.quiz.enums.SourceType;
import com.quizzy.module.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 错题本：入本与移出的判定由 {@link QuizService#answer} 维护 question_stat，
 * 这里负责展示、手动移出与发起错题重练。
 */
@Service
@RequiredArgsConstructor
public class WrongBookService {

    private final QuestionService questionService;
    private final QuestionStatMapper questionStatMapper;
    private final QuizService quizService;

    public PageResult<QuestionListItemVO> page(long pageNo, long pageSize, Long userId) {
        QuestionQueryDTO query = new QuestionQueryDTO();
        query.setPage(pageNo);
        query.setSize(pageSize);
        query.setOnlyWrong(true);
        return questionService.page(query, userId);
    }

    /**
     * 手动移出错题本，并记为已达掌握标准，避免下次答错又立刻进本。
     */
    public void remove(Long questionId, Long userId) {
        QuestionStat stat = questionStatMapper.selectOne(new LambdaQueryWrapper<QuestionStat>()
                .eq(QuestionStat::getUserId, userId)
                .eq(QuestionStat::getQuestionId, questionId));
        if (stat == null) {
            return;
        }
        stat.setInWrongBook(0);
        stat.setConsecutiveCorrect(3);
        questionStatMapper.updateById(stat);
    }

    public Long practice(Integer count, Long userId) {
        QuizStartDTO dto = new QuizStartDTO();
        dto.setSourceType(SourceType.WRONG_BOOK);
        dto.setCount(count == null || count <= 0 ? 20 : count);
        return quizService.start(dto, userId);
    }
}
