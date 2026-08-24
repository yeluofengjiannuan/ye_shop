package com.itxindeshang.common.mapstruct;

import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.dto.CategoryDTO;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.dto.ProductUpdateDTO;
import com.itxindeshang.pojo.entity.Category;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.entity.SysUser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略字段不匹配警告
        unmappedSourcePolicy = ReportingPolicy.IGNORE)  // 忽略源对象多余字段)
public interface CopyMapper {

    UserInfo sysUserToUserInfo(SysUser sysUser);

    Category categoryDTOToCategroy(CategoryDTO categoryDTO);

    Product productDTOToProduct(ProductDTO productDTO);

    Product productUpdateDTOToProduct(ProductUpdateDTO productUpdateDTO);
}
