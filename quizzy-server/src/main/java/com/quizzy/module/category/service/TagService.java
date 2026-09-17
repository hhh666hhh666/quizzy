package com.quizzy.module.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quizzy.module.category.entity.Tag;
import com.quizzy.module.category.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<Tag> list() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
    }

    /**
     * 标签全局共享：同名复用，不存在则创建。
     */
    public List<Long> resolveIds(Collection<String> names) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                if (StringUtils.hasText(name) && name.length() <= 64) {
                    cleaned.add(name.trim());
                }
            }
        }
        List<Long> ids = new ArrayList<>();
        for (String name : cleaned) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            ids.add(tag.getId());
        }
        return ids;
    }
}
