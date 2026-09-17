package com.quizzy.module.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quizzy.module.quiz.entity.QuizAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizAnswerMapper extends BaseMapper<QuizAnswer> {
}
