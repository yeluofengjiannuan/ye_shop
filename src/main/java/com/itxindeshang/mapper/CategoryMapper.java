package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    int countByIds(List<Long> categoryIds);

    /**
     * 根据优惠券ID查出所有适用的分类ID（一级分类会自动展开子类）
     */
    List<Long> selectApplicableCategoryIds(@Param("couponId") Long couponId);
}
