package com.quizzy.module.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    public List<Category> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public Category create(String name, Integer sort) {
        String trimmed = requireName(name);
        if (exists(trimmed, null)) {
            throw new BusinessException(ResultCode.CONFLICT, "分类已存在");
        }
        Category category = new Category();
        category.setName(trimmed);
        category.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(category);
        return category;
    }

    public Category rename(Long id, String name, Integer sort) {
        Category category = requireCategory(id);
        String trimmed = requireName(name);
        if (exists(trimmed, id)) {
            throw new BusinessException(ResultCode.CONFLICT, "分类已存在");
        }
        category.setName(trimmed);
        if (sort != null) {
            category.setSort(sort);
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCategory(id);
        questionMapper.clearCategory(id);
        categoryMapper.deleteById(id);
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name) || name.length() > 64) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类名称不能为空且不超过 64 个字符");
        }
        return name.trim();
    }

    private boolean exists(String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>().eq(Category::getName, name);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        return categoryMapper.selectCount(wrapper) > 0;
    }
}
