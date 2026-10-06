package com.quizzy.module.paper.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 往已有固定卷**追加**题目的入参。
 *
 * <p>与 {@link PaperSaveDTO} 的区别：那个是**全量替换**（连规则卷一起管），
 * 这个是**并入**——只加不减，已有的题原样留着。
 */
@Data
public class PaperAppendDTO {

    @NotEmpty(message = "请选择要加入的题目")
    private List<Long> questionIds;
}
