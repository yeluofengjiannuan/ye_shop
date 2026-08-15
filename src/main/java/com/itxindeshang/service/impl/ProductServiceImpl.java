package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.DataConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.entity.Category;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.service.CategoryService;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private CopyMapper copyMapper;

    @Resource
    private CategoryService categoryService;

    /**
     *  新增商品
     * @param productDTO
     * @return
     */
    @Override
    public Result addProduct(ProductDTO productDTO) {
        Product product = copyMapper.productDTOToProduct(productDTO);
        Category category = categoryService.getById(product.getCategoryId());
        if (category == null) {
            return Result.error(MessageConstant.CATEGORY_NOT_FOUND);
        }
        Long categoryParentId = category.getParentId();
        //商品不得出现在第一级分类
        //TODO:这是二级分类下才能这样，后续考虑增加分级代码无法复用，考虑优化
        if (DataConstant.ZERO_LONG.equals(categoryParentId)) {
            return Result.error(MessageConstant.PRODUCT_CATEGORY_INVALID);
        }
        boolean isSuccess = save(product);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        return Result.success(product);
    }
}
