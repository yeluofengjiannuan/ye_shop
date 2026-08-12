package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.itxindeshang.common.constant.CaffeineConstant;
import com.itxindeshang.common.constant.DataConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.mapper.CategoryMapper;
import com.itxindeshang.pojo.dto.CategoryDTO;
import com.itxindeshang.pojo.entity.Category;
import com.itxindeshang.service.CategoryService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Resource
    private Cache<String, List<Category>> categoryTreeCache;

    @Resource
    private  Cache<String, Map<Long, Category>> categoryMapCache;

    @Resource
    private CopyMapper copyMapper;

    /**
     * 递归 为分类树 set List<Category> children
     * 最终就是很完美的层级
     * TODO:后续看需要写在service然后overide吗
     * @param allCategories
     * @param parentCategories
     */
    private void buildCategoryTree(List<Category> allCategories, List<Category> parentCategories) {
        if (CollectionUtils.isEmpty(allCategories) || CollectionUtils.isEmpty(parentCategories)) {
            return;
        }
        //TODO：把这个抽出来
        Map<Long, List<Category>> groupMap = allCategories.stream().collect(Collectors.groupingBy(Category::getParentId));
        List<Category> nextCategoriesList = new ArrayList<>();
        parentCategories.forEach(category -> {
            List<Category> childrenCategories = groupMap.getOrDefault(category.getId(), new ArrayList<>());
            category.setChildren(childrenCategories);
            nextCategoriesList.addAll(childrenCategories);
        });
        buildCategoryTree(allCategories, nextCategoriesList);
    }
    /**
     * 获取分类树
     * @return
     */
    public List<Category> categorytree() {
        // 如果缓存为空，触发一次刷新
        //TODO:缓存穿透
        List<Category> tree = categoryTreeCache.getIfPresent(CaffeineConstant.CACHE_KEY_CATEGORY_TREE);
        if (tree == null) {
            refreshCategoryCache();
            tree = categoryTreeCache.getIfPresent(CaffeineConstant.CACHE_KEY_CATEGORY_TREE);
        }
        return tree;
    }
    /**
     * 统一刷新分类缓存（只查一次库！）
     */
    public void refreshCategoryCache() {
        // 1. 只查一次数据库
        List<Category> allCategories = lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .list();

        // 2. 生成扁平 Map（利用 Stream 瞬间完成）
        Map<Long, Category> flatMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (v1, v2) -> v1));//这里

        // 3. 生成树形结构
        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId().equals(DataConstant.ZERO_LONG))
                .toList();
        buildCategoryTree(allCategories, rootCategories); // 复用你原来的递归方法

        // 4. 同时写入两个缓存（没有循环依赖，没有二次查库）
        categoryTreeCache.put(CaffeineConstant.CACHE_KEY_CATEGORY_TREE, rootCategories);
        categoryMapCache.put(CaffeineConstant.CACHE_KEY_CATEGORY_MAP, flatMap);
    }

    /**
     * 清除树缓存
     */
    public void invalidateCache() {
        //清除缓存
        categoryTreeCache.invalidate(CaffeineConstant.CACHE_KEY_CATEGORY_TREE);
        categoryMapCache.invalidate(CaffeineConstant.CACHE_KEY_CATEGORY_MAP);
    }
    /**
     * 获取子节点
     * @param id
     * @return
     */
    private Category getCategoryChildren(Long id) {
        Map<Long, Category> categoryMap = categoryMapCache.getIfPresent(CaffeineConstant.CACHE_KEY_CATEGORY_MAP);
        if (CollectionUtils.isEmpty(categoryMap)) {
            refreshCategoryCache();
            categoryMap = categoryMapCache.getIfPresent(CaffeineConstant.CACHE_KEY_CATEGORY_MAP);
        }
        Category category = categoryMap.get(id);
        return category;
    }
    /**
     * 删除分类
     * @param categoryId
     * @return
     */
    @Override
    public Result deleteById(String categoryId) {
        Long id = Long.parseLong(categoryId);
        Category category = getCategoryChildren(id);
        if (!CollectionUtils.isEmpty(category.getChildren())) {
            return Result.error(MessageConstant.SQL_MESSAGE_DELETE_ERROR);
        }
        boolean isSuccess = removeById(id);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_DELETE_ERROR);
        }
        invalidateCache();
        return Result.success();
    }

    /**
     * 更新分类
     * TODO:更新parentID是最要命的，要想好逻辑,最后的重复可以看着修改更优雅
     * @param id
     * @param categoryDTO
     * @return
     */
    @Override
    public Result updateCategory(String id, CategoryDTO categoryDTO) {
        Long categoryId = Long.parseLong(id);
        Category category = copyMapper.categoryDTOToCategroy(categoryDTO);

        Category categoryCache = getCategoryChildren(categoryId);
        if (categoryCache == null) {
            Category selectedCategory = getById(categoryId);
            if (!category.getParentId().equals(selectedCategory.getParentId())) {
                return Result.error(MessageConstant.SQL_MESSAGE_UPDATE_PARENTID_ERROR);
            }
            category.setId(categoryId);
            boolean isSuccess = updateById(category);
            if (!isSuccess) {
                return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
            }
            invalidateCache();
            return Result.success(category);
        }
        if (!category.getParentId().equals(categoryCache.getParentId())) {
            return Result.error(MessageConstant.SQL_MESSAGE_UPDATE_PARENTID_ERROR);
        }
        category.setId(categoryId);
        boolean isSuccess = updateById(category);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        invalidateCache();
        return Result.success(category);
    }

    /**
     * 展示分类树
     * @return
     */
    @Override
    public Result showCategorytree() {
        List<Category> tree =categorytree();
        return Result.success(tree);
    }

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @Override
    public Result addCategory(CategoryDTO categoryDTO) {
        Category category = copyMapper.categoryDTOToCategroy(categoryDTO);
        boolean isSuccess = save(category);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        //清除树缓存
        invalidateCache();
        // 4. 返回新增节点的子节点（或者返回成功提示）
        Category savedCategory = getCategoryChildren(category.getId());
        return Result.success(savedCategory);
    }
}
