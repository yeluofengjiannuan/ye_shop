package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import com.itxindeshang.pojo.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    //TODO:时间的对比逻辑可能需要优化
    /**
     *
     * @param categoryId 分类Id
     * @param sortField  排序字段
     * @param productId  上一次查询商品Id
     * @param sortValue  上一次查询关于排序字段的值
     * @param isAsc      查询顺序
     * @param querySize  一次查询数量
     * @return
     */
    List<ProductVO> getCategoryProductList(
            @Param("categoryId") Long categoryId,
            @Param("sortField")String sortField,
            @Param("productId")Long productId,
            @Param("sortValue")String sortValue,
            @Param("isAsc")Boolean isAsc,
            @Param("querySize")Integer querySize
    );
}
