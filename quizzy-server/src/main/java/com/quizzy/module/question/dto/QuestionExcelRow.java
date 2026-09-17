package com.quizzy.module.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入导出的行模型，列顺序与模板表头一一对应。
 */
@Data
public class QuestionExcelRow {

    @ExcelProperty(index = 0)
    private String type;

    @ExcelProperty(index = 1)
    private String stem;

    @ExcelProperty(index = 2)
    private String optionA;

    @ExcelProperty(index = 3)
    private String optionB;

    @ExcelProperty(index = 4)
    private String optionC;

    @ExcelProperty(index = 5)
    private String optionD;

    @ExcelProperty(index = 6)
    private String optionE;

    @ExcelProperty(index = 7)
    private String optionF;

    @ExcelProperty(index = 8)
    private String answer;

    @ExcelProperty(index = 9)
    private String analysis;

    @ExcelProperty(index = 10)
    private String difficulty;

    @ExcelProperty(index = 11)
    private String score;

    @ExcelProperty(index = 12)
    private String category;

    @ExcelProperty(index = 13)
    private String tags;
}
