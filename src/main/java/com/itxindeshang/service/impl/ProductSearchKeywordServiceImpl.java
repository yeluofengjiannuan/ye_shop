package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.mapper.ProductSearchKeywordMapper;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import com.itxindeshang.service.ProductSearchKeywordService;
import com.itxindeshang.util.CaffenineUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProductSearchKeywordServiceImpl extends ServiceImpl<ProductSearchKeywordMapper, ProductSearchKeyword> implements ProductSearchKeywordService {
    @Resource
    private CaffenineUtils caffeineUtils;

    /**
     * 用户获取热门搜索关键词列表
     * @return
     */
    @Override
    public Result<List<String>> getProductSearchKeywordListUser() {
        //caffeine查询缓存
        List<String> hotProductSearchKeyword = caffeineUtils.getHotProductSearchKeyword();
        //打乱顺序
        Collections.shuffle(hotProductSearchKeyword);
        //查询5条数据
        List<String> resultList = hotProductSearchKeyword.stream().limit(5).toList();
        return Result.success(resultList);
    }
}
