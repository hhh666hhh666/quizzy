package com.quizzy.module.question.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionExcelRow;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.dto.QuestionSaveDTO;
import com.quizzy.module.question.enums.Difficulty;
import com.quizzy.module.question.enums.QuestionType;
import com.quizzy.module.question.vo.ImportErrorVO;
import com.quizzy.module.question.vo.ImportResultVO;
import com.quizzy.module.question.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private static final List<String> HEAD = List.of(
            "题型", "题干", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
            "答案", "解析", "难度", "分值", "分类", "标签");

    private final QuestionService questionService;
    private final CategoryMapper categoryMapper;

    public ImportResultVO importExcel(InputStream inputStream, Long userId) {
        List<QuestionExcelRow> rows = EasyExcel.read(inputStream)
                .head(QuestionExcelRow.class)
                .sheet()
                .doReadSync();
        List<ImportErrorVO> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            QuestionExcelRow row = rows.get(i);
            if (row == null || !StringUtils.hasText(row.getStem())) {
                continue;
            }
            total++;
            try {
                questionService.save(toSaveDTO(row), userId);
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(i + 2, row.getStem(), e.getMessage()));
            } catch (Exception e) {
                errors.add(new ImportErrorVO(i + 2, row.getStem(), "格式不正确：" + e.getMessage()));
            }
        }
        return ImportResultVO.of(total, success, errors);
    }

    public ImportResultVO importJson(List<QuestionImportDTO> items, Long userId) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException("导入内容为空");
        }
        List<ImportErrorVO> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < items.size(); i++) {
            QuestionImportDTO item = items.get(i);
            try {
                questionService.save(toSaveDTO(item), userId);
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(i + 1, item.getStem(), e.getMessage()));
            } catch (Exception e) {
                errors.add(new ImportErrorVO(i + 1, item.getStem(), "格式不正确：" + e.getMessage()));
            }
        }
        return ImportResultVO.of(items.size(), success, errors);
    }

    public void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream)
                .head(headRows())
                .sheet("题目")
                .doWrite(Collections.emptyList());
    }

    public void exportExcel(List<Long> ids, Long userId, OutputStream outputStream) {
        List<List<String>> data = questionService.findForExport(userId, ids).stream()
                .map(this::toRow)
                .toList();
        EasyExcel.write(outputStream)
                .head(headRows())
                .sheet("题目")
                .doWrite(data);
    }

    public List<QuestionImportDTO> exportJson(List<Long> ids, Long userId) {
        return questionService.findForExport(userId, ids).stream()
                .map(this::toJsonDTO)
                .collect(Collectors.toList());
    }

    private List<List<String>> headRows() {
        return HEAD.stream().map(Collections::singletonList).collect(Collectors.toList());
    }

    private List<String> toRow(QuestionVO vo) {
        List<String> row = new ArrayList<>(List.of(
                vo.getType() == null ? "" : vo.getType().name().toLowerCase(),
                vo.getStem() == null ? "" : vo.getStem(),
                "", "", "", "", "", "",
                String.join(",", vo.getAnswers()),
                vo.getAnalysis() == null ? "" : vo.getAnalysis(),
                vo.getDifficulty() == null ? "" : vo.getDifficulty().name().toLowerCase(),
                String.valueOf(vo.getScore() == null ? 1 : vo.getScore()),
                vo.getCategoryName() == null ? "" : vo.getCategoryName(),
                vo.getTags().stream().map(t -> t.name()).collect(Collectors.joining(","))
        ));
        for (int i = 0; i < vo.getOptions().size() && i < 6; i++) {
            row.set(2 + i, vo.getOptions().get(i).content());
        }
        return row;
    }

    private QuestionImportDTO toJsonDTO(QuestionVO vo) {
        QuestionImportDTO dto = new QuestionImportDTO();
        dto.setType(vo.getType() == null ? null : vo.getType().name().toLowerCase());
        dto.setStem(vo.getStem());
        dto.setOptions(vo.getOptions().stream()
                .map(o -> {
                    OptionDTO option = new OptionDTO();
                    option.setLabel(o.label());
                    option.setContent(o.content());
                    return option;
                })
                .collect(Collectors.toList()));
        dto.setAnswer(vo.getAnswers());
        dto.setAnalysis(vo.getAnalysis());
        dto.setDifficulty(vo.getDifficulty() == null ? null : vo.getDifficulty().name().toLowerCase());
        dto.setScore(vo.getScore());
        dto.setCategory(vo.getCategoryName());
        dto.setTags(vo.getTags().stream().map(t -> t.name()).collect(Collectors.toList()));
        return dto;
    }

    private QuestionSaveDTO toSaveDTO(QuestionExcelRow row) {
        QuestionSaveDTO dto = new QuestionSaveDTO();
        dto.setType(parseType(row.getType()));
        dto.setStem(row.getStem());
        dto.setAnalysis(row.getAnalysis());
        dto.setDifficulty(Difficulty.ofOrDefault(row.getDifficulty()));
        dto.setScore(parseScore(row.getScore()));
        dto.setCategoryId(resolveCategoryId(row.getCategory()));
        dto.setAnswers(splitAnswer(row.getAnswer()));
        dto.setOptions(buildOptions(row));
        dto.setTags(splitTags(row.getTags()));
        return dto;
    }

    private QuestionSaveDTO toSaveDTO(QuestionImportDTO item) {
        QuestionSaveDTO dto = new QuestionSaveDTO();
        dto.setType(parseType(item.getType()));
        dto.setStem(item.getStem());
        dto.setAnalysis(item.getAnalysis());
        dto.setDifficulty(Difficulty.ofOrDefault(item.getDifficulty()));
        dto.setScore(item.getScore() == null ? 1 : item.getScore());
        dto.setCategoryId(resolveCategoryId(item.getCategory()));
        dto.setAnswers(item.getAnswer());
        if (CollectionUtils.isEmpty(item.getOptions())) {
            throw new BusinessException("选项不能为空");
        }
        dto.setOptions(item.getOptions());
        dto.setTags(item.getTags());
        return dto;
    }

    private List<OptionDTO> buildOptions(QuestionExcelRow row) {
        String[] labels = {"A", "B", "C", "D", "E", "F"};
        String[] contents = {
                row.getOptionA(), row.getOptionB(), row.getOptionC(),
                row.getOptionD(), row.getOptionE(), row.getOptionF()
        };
        List<OptionDTO> options = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (StringUtils.hasText(contents[i])) {
                OptionDTO option = new OptionDTO();
                option.setLabel(labels[i]);
                option.setContent(contents[i]);
                options.add(option);
            }
        }
        return options;
    }

    private QuestionType parseType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("题型不能为空");
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "SINGLE", "单选", "单选题" -> QuestionType.SINGLE;
            case "MULTI", "MULTIPLE", "多选", "多选题" -> QuestionType.MULTI;
            case "JUDGE", "判断", "判断题" -> QuestionType.JUDGE;
            default -> throw new BusinessException("未知题型：" + value);
        };
    }

    private List<String> splitAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return Collections.emptyList();
        }
        return Arrays.stream(answer.split("[,，、]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split("[,，、]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Integer parseScore(String score) {
        if (!StringUtils.hasText(score)) {
            return 1;
        }
        return Integer.parseInt(score.trim());
    }

    private Long resolveCategoryId(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            return null;
        }
        String name = categoryName.trim();
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getName, name));
        if (category != null) {
            return category.getId();
        }
        Category created = new Category();
        created.setName(name);
        created.setSort(0);
        categoryMapper.insert(created);
        return created.getId();
    }
}
