package com.quizzy.module.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quizzy.module.question.entity.QuestionStat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionStatMapper extends BaseMapper<QuestionStat> {
}
