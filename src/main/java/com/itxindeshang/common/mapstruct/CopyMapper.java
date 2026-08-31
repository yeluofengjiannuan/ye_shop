package com.itxindeshang.common.mapstruct;

import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.dto.*;
import com.itxindeshang.pojo.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略字段不匹配警告
        unmappedSourcePolicy = ReportingPolicy.IGNORE)  // 忽略源对象多余字段)
public interface CopyMapper {

    UserInfo sysUserToUserInfo(SysUser sysUser);

    Category categoryDTOToCategroy(CategoryDTO categoryDTO);

    Product productDTOToProduct(ProductDTO productDTO);

    Product productUpdateDTOToProduct(ProductUpdateDTO productUpdateDTO);

    @Mapping(source = "cart.id", target = "id")
    @Mapping(source = "cart.userId", target = "userId")
    @Mapping(source = "cart.productId", target = "productId")
    @Mapping(source = "cart.specId", target = "specId")
    @Mapping(source = "cart.quantity", target = "quantity")
    @Mapping(source = "cart.createTime", target = "createTime")
    @Mapping(source = "cart.updateTime", target = "updateTime")

    @Mapping(source = "detail.price", target = "price")
    @Mapping(source = "detail.stock", target = "stock")
    @Mapping(source = "detail.specText", target = "specText")
    @Mapping(source = "detail.productName", target = "productName")
    @Mapping(source = "detail.productImage", target = "productImage")
    CartItem toCartItem(Cart cart, CartProductSpecDTO detail);

    Address addressDTOToAddress(AddressDTO addressDTO);
}
