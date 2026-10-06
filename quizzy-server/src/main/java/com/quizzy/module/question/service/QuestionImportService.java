package com.quizzy.module.question.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.paper.dto.PaperSaveDTO;
import com.quizzy.module.paper.enums.PaperMode;
import com.quizzy.module.paper.service.PaperService;
import com.quizzy.module.question.dto.OptionDTO;
import com.quizzy.module.question.dto.QuestionExcelRow;
import com.quizzy.module.question.dto.QuestionImportDTO;
import com.quizzy.module.question.dto.QuestionQueryDTO;
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
    private final PaperService paperService;

    /**
     * 导入 Excel。
     *
     * @param paperTitle 给了卷名就**顺带把本次成功导入的题装进一张新固定卷**（ADR 0026）；
     *                   留空则只导入题目。元信息走查询参数，不动裸数组契约（ADR 0006 Amendment 1）。
     */
    public ImportResultVO importExcel(InputStream inputStream, Long userId, String paperTitle) {
        List<QuestionExcelRow> rows = EasyExcel.read(inputStream)
                .head(QuestionExcelRow.class)
                .sheet()
                .doReadSync();
        List<ImportErrorVO> errors = new ArrayList<>();
        List<Long> createdIds = new ArrayList<>();
        int total = 0;
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            QuestionExcelRow row = rows.get(i);
            if (row == null || !StringUtils.hasText(row.getStem())) {
                continue;
            }
            total++;
            try {
                createdIds.add(questionService.save(toSaveDTO(row), userId));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(i + 2, row.getStem(), e.getMessage()));
            } catch (Exception e) {
                errors.add(new ImportErrorVO(i + 2, row.getStem(), "格式不正确：" + e.getMessage()));
            }
        }
        return ImportResultVO.of(total, success, errors, createPaperIfRequested(paperTitle, createdIds, userId));
    }

    /** 同 {@link #importExcel}，只是入参是已经解析好的 JSON 裸数组。 */
    public ImportResultVO importJson(List<QuestionImportDTO> items, Long userId, String paperTitle) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException("导入内容为空");
        }
        List<ImportErrorVO> errors = new ArrayList<>();
        List<Long> createdIds = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < items.size(); i++) {
            QuestionImportDTO item = items.get(i);
            try {
                createdIds.add(questionService.save(toSaveDTO(item), userId));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(i + 1, item.getStem(), e.getMessage()));
            } catch (Exception e) {
                errors.add(new ImportErrorVO(i + 1, item.getStem(), "格式不正确：" + e.getMessage()));
            }
        }
        return ImportResultVO.of(items.size(), success, errors, createPaperIfRequested(paperTitle, createdIds, userId));
    }

    /**
     * 按需把「本次成功导入的题」装进一张**新**固定卷。三条边界：
     *
     * <ul>
     *   <li>没给卷名（或只有空白）→ 不建卷。卷名是「要不要建卷」的开关本身；</li>
     *   <li>一题都没成功 → 不建卷。一张空卷对用户没有意义，只会污染试卷列表；</li>
     *   <li>卷名**不去重** → 每次导入都是新卷，同名就同名（{@code paper.title} 本就没有唯一键）。</li>
     * </ul>
     *
     * <p>⚠️ 题是**逐条**入库的（ADR 0005 的部分成功语义），而建卷是**最后一步**：
     * 这一步若因数据库故障失败，已入库的题不会回滚——调用方拿到的是错误信封，可题其实已经进去了。
     * 现实里只有数据库故障能触发它，所以不额外包一层容错。
     */
    private Long createPaperIfRequested(String paperTitle, List<Long> createdIds, Long userId) {
        if (!StringUtils.hasText(paperTitle) || createdIds.isEmpty()) {
            return null;
        }
        PaperSaveDTO dto = new PaperSaveDTO();
        dto.setTitle(paperTitle.trim());
        dto.setMode(PaperMode.FIXED);
        dto.setQuestionIds(createdIds);
        return paperService.save(dto, userId);
    }

    public void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream)
                .head(headRows())
                .sheet("题目")
                .doWrite(Collections.emptyList());
    }

    /**
     * 导出为 Excel。{@code query} 就是列表页那套筛选条件——**导出跟随筛选**，
     * 但**不跟随分页**：导出的是全部匹配的题目。
     */
    public void exportExcel(QuestionQueryDTO query, Long userId, List<Long> ids, OutputStream outputStream) {
        List<List<String>> data = questionService.findForExport(query, userId, ids).stream()
                .map(this::toRow)
                .toList();
        EasyExcel.write(outputStream)
                .head(headRows())
                .sheet("题目")
                .doWrite(data);
    }

    /** 导出为 JSON。筛选语义同 {@link #exportExcel}。 */
    public List<QuestionImportDTO> exportJson(QuestionQueryDTO query, Long userId, List<Long> ids) {
        return questionService.findForExport(query, userId, ids).stream()
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
