package com.quizzy.module.question.converter;

import com.quizzy.module.question.entity.Question;
import com.quizzy.module.question.vo.QuestionListItemVO;
import com.quizzy.module.question.vo.QuestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface QuestionConverter {

    @Mapping(target = "answers", ignore = true)
    QuestionVO toVO(Question question);

    List<QuestionListItemVO> toListItemVO(List<Question> questions);
}
