package com.itxindeshang.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.itxindeshang.common.constant.CaffeineConstant;
import com.itxindeshang.mapper.CategoryMapper;
import com.itxindeshang.mapper.ProductSearchKeywordMapper;
import com.itxindeshang.pojo.entity.Category;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import com.itxindeshang.pojo.enums.CommonStatus;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class CaffenineConfig {
    @Resource
    private ProductSearchKeywordMapper productSearchKeywordMapper;
    //TODO:依赖循环了
    @Resource
    private CategoryMapper categoryMapper;

    /**
     * 商品搜索关键词缓存
     */
    @Bean
    public LoadingCache<String, List<String>> hotProductSearchKeywordCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                // .expireAfterWrite(12, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                           @Override
                           public @Nullable List<String> load(String key) throws IllegalArgumentException {
                               if (StringUtils.equals(key, CaffeineConstant.CACHE_KEY_HOT_PRODUCT_SEARCH_KEYWORD)) {
                                   return getHotProductSearchKeywordListUser();
                               }
                               throw new IllegalArgumentException(CaffeineConstant.CACHE_KEY_NOT_VALID_ERROR);
                           }
                       }
                );
    }

    /**
     *分类树(一级分类,二级分类)缓存
     *//*
    @Bean
    public LoadingCache<String, List<com.itxindeshang.pojo.entity.Category>> categoryTreeCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .build(new CacheLoader<>() {
                    @Override
                    public @Nullable List<Category> load(String key) throws IllegalArgumentException {
                        if (StringUtils.equals(key, CaffeineConstant.CACHE_KEY_CATEGORY_TREE)) {
//                            return categoryService.getCategroytreeCache();
                        }
                        throw new IllegalArgumentException(CaffeineConstant.CACHE_KEY_NOT_VALID_ERROR);
                    }
                });
    }*/
    //TODO:把这个方法抽出来，utils也好
    private List<String> getHotProductSearchKeywordListUser() {
        LambdaQueryWrapper<ProductSearchKeyword> productSearchKeywordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        productSearchKeywordLambdaQueryWrapper
                .eq(ProductSearchKeyword::getIsShow, CommonStatus.ACTIVE.getNumber())
                .eq(ProductSearchKeyword::getIsHot, CommonStatus.ACTIVE.getNumber());
        List<ProductSearchKeyword> productSearchKeywords = productSearchKeywordMapper.selectList(productSearchKeywordLambdaQueryWrapper);
        if (CollectionUtils.isEmpty(productSearchKeywords)) {
            return Collections.emptyList();
        }
        return productSearchKeywords.stream().map(ProductSearchKeyword::getKeyword).collect(Collectors.toList());
    }
    /*@Bean
    public LoadingCache<String, Map<Long, Category>> categoryMapCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .build(new CacheLoader<>() {
                    @Override
                    public @Nullable Map<Long, Category> load(String key) throws IllegalArgumentException {
                        if (StringUtils.equals(key, CaffeineConstant.CACHE_KEY_CATEGORY_MAP)) { // 假设你定义了这个常量
//                            return categoryService.getCategoryMapCache();
                        }
                        throw new IllegalArgumentException(CaffeineConstant.CACHE_KEY_NOT_VALID_ERROR);
                    }
                });
    }*/

    // 树缓存
    @Bean
    public Cache<String, List<Category>> categoryTreeCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .build(); // 注意：这里没有 CacheLoader
    }

    // Map缓存
    @Bean
    public Cache<String, Map<Long, Category>> categoryMapCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .build(); // 注意：这里也没有 CacheLoader
    }

}
