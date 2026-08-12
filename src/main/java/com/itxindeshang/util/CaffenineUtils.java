package com.itxindeshang.util;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.itxindeshang.common.constant.CaffeineConstant;
import com.itxindeshang.pojo.entity.Category;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaffenineUtils {
    @Resource
    private LoadingCache<String, List<String>> hotProductSearchKeywordCache;

    /*@Resource
    private LoadingCache<String,List<Category>> categoryTreeCache   ;*/


    /**
     * 查询热门搜索关键词
     */
    public  List<String> getHotProductSearchKeyword() {
        return hotProductSearchKeywordCache.get(CaffeineConstant.CACHE_KEY_HOT_PRODUCT_SEARCH_KEYWORD);
    }

    /**
     * 查询分类树
     * @return
     *//*
    public List<Category> getCategoryTree() {
        return categoryTreeCache.get(CaffeineConstant.CACHE_KEY_CATEGORY_TREE);
    }

    *//**
     * 删除分类树
     *//*
    public void invalidateCategoryTree() {
        categoryTreeCache.invalidate(CaffeineConstant.CACHE_KEY_CATEGORY_TREE);
    }*/
}
