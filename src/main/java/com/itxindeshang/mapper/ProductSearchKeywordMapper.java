package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSearchKeywordMapper extends BaseMapper<ProductSearchKeyword> {
    /**
     * 清空全表（忽略防全表删除拦截器）
     */
    @InterceptorIgnore(blockAttack = "true")
    @Delete("DELETE FROM product_search_keyword")
    void deleteAll();
}
