package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CategoryDTO;
import com.itxindeshang.pojo.entity.Category;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {
    Result showCategorytree();

    Result addCategory(CategoryDTO categoryDTO);

    void refreshCategoryCache();

    Result deleteById(Long categoryId);

    Result updateCategory(Long categoryId, CategoryDTO categoryDTO);
}
