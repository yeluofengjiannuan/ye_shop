package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.dto.CartProductSpecDTO;
import com.itxindeshang.pojo.entity.Cart;
import com.itxindeshang.pojo.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    Integer getCartQuantityOnly(@Param("userId") String userId,
                            @Param("productId") Long productId,
                            @Param("specId") Long specId);

    Integer checkProductStatus(Long productId, Long specId);

    List<CartProductSpecDTO> selectSpecsWithProduct(Set<Long> specIds);
}
