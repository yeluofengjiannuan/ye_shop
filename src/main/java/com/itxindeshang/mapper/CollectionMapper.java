package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.ProductCollection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CollectionMapper extends BaseMapper<ProductCollection> {
}
