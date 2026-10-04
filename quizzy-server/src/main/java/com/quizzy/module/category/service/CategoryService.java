package com.quizzy.module.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;
import com.quizzy.module.category.entity.Category;
import com.quizzy.module.category.mapper.CategoryMapper;
import com.quizzy.module.question.entity.Question;
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

    /**
     * 按名字解析出目标分类：**同名已存在（哪怕是别人的）就复用**，否则新建。
     *
     * <p>分类是全体共用的（CONTEXT.md），所以「撞别人的名字」不是冲突而是**合并**——
     * 同一个名字在语义上就是同一个分类。这也是这里不报 409 的原因。
     */
    public Category resolveByName(String name) {
        String trimmed = requireName(name);
        Category existing = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getName, trimmed).last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Category created = new Category();
        created.setName(trimmed);
        created.setSort(0);
        categoryMapper.insert(created);
        return created;
    }

    /**
     * 改分类名：**只迁移调用者自己的题目**。
     *
     * <p>⚠️ 为什么不是「原地改名」：分类是共享的，原地改会让**所有人**的题目都换名字。
     * 这里只动调用者自己的归属——别人的题目仍挂在原分类上，于是原分类
     * **不会**因为这次改名被删（它还有别人的引用）。
     *
     * <p>净效果：「我把我的题挪到了另一个分类」；若目标名字已存在，就是**合并**。
     */
    @Transactional(rollbackFor = Exception.class)
    public Category moveTo(Long categoryId, String newName, Long userId) {
        Category source = requireCategory(categoryId);
        Category target = resolveByName(newName);
        if (source.getId().equals(target.getId())) {
            return source;
        }
        questionMapper.update(null, new LambdaUpdateWrapper<Question>()
                .set(Question::getCategoryId, target.getId())
                .eq(Question::getCategoryId, source.getId())
                .eq(Question::getOwnerId, userId));
        pruneIfOrphan(source.getId());
        return target;
    }

    /**
     * 分类无人引用时自动删除。
     *
     * <p>判据只查 {@code question} 表（软删的题目不算引用）。
     * ⚠️ {@code paper.rule_json} 里也存着 categoryId，但**不需要**在这里考虑：
     * 规则卷按分类抽题，能抽到题 ⟺ 存在该分类的可见题目 ⟺ 分类有引用；
     * 所以「引用为 0」时引用它的规则卷**本来也抽不到题**。
     * 而引用一旦真的失效，`PaperService#selectQuestionsByRule` 会让那条条件
     * **自动失效**（而不是让整张卷抽不到题）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void pruneIfOrphan(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            return;
        }
        long references = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, categoryId));
        if (references == 0) {
            delete(categoryId);
        }
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
