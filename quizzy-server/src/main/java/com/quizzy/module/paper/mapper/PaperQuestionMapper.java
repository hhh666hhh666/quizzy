package com.quizzy.module.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quizzy.module.paper.entity.PaperQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {
}
