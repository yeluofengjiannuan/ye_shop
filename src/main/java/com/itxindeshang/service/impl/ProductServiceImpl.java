package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.DataConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.entity.Category;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import com.itxindeshang.pojo.vo.ProductVO;
import com.itxindeshang.service.CategoryService;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private ProductMapper productMapper;

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
    /**
     * 游标查询分类下所有商品列表
     * @return
     */
    @Override
    public Result<CursorCommonResult> getCategoryProductList(CursorCommonEntity cursorCommonEntity, Long categoryId) {
        //TODO: 确保查询categoryId是二级分类
        //在值相同时确保不重复
        Long sortId = cursorCommonEntity.getSortId();
        String sortType = cursorCommonEntity.getSortType();
        String sortValue = cursorCommonEntity.getSortValue();
        ProductSortTypeEnum productSortTypeEnum = ProductSortTypeEnum.getByValue(sortType);
        //上一次查询的最后值/定位
        sortValue = ProductSortTypeEnum.filterFormatSortValue(productSortTypeEnum, sortValue);
        Integer querySize = cursorCommonEntity.getQuerySize();
        String sortField = productSortTypeEnum.getSortField();
        String dbValue = productSortTypeEnum.getDbValue();//TODO:未使用字段，后续可以考虑删除或者功能复用
        boolean isAsc = productSortTypeEnum.getCommonSortTypeEnum().isAsc();
        //dbvalue,isAsc?,productId, querySize,,desc
        List<ProductVO> queryList = productMapper.getCategoryProductList(categoryId, sortField, sortId, sortValue, isAsc, querySize);
        return getCursorCommonResult(queryList,querySize, productSortTypeEnum, sortType);
    }

    /**
     * 游标分类查询简介商品列表
     * @param cursorCommonEntity 分类游标通用实体
     * @param keyword 关键词
     * @return 分类游标通用实体
     */
    //TODO:空传判断AOP
    @Override
    public Result<CursorCommonResult> searchProductList(CursorCommonEntity cursorCommonEntity, String keyword) {
        Integer querySize = cursorCommonEntity.getQuerySize();
        String sortType = cursorCommonEntity.getSortType();
        Long sortId = cursorCommonEntity.getSortId();
        String sortValue = cursorCommonEntity.getSortValue();
        ProductSortTypeEnum productSortTypeEnum = ProductSortTypeEnum.getByValue(sortType);
        String sortField = productSortTypeEnum.getSortField();
        sortValue = ProductSortTypeEnum.filterFormatSortValue(productSortTypeEnum, sortValue);
        boolean isAsc = productSortTypeEnum.getCommonSortTypeEnum().isAsc();
        List<ProductVO> queryList = productMapper.searchProductList(sortField, sortValue,sortId, isAsc, querySize, keyword);

        return getCursorCommonResult(queryList,querySize, productSortTypeEnum, sortType);

    }

    /**
     * 游标结果封装
     * @param queryList 查询列表
     * @param querySize 查询数量
     * @param productSortTypeEnum 商品排序种类枚举
     * @param sortType 排序种类字符串
     * @return 游标结果
     */
    private Result<CursorCommonResult> getCursorCommonResult(List<ProductVO> queryList, Integer querySize, ProductSortTypeEnum productSortTypeEnum, String sortType) {
        boolean isEnd = false;
        if (CollectionUtils.isEmpty(queryList)) {
            CursorCommonResult result = CursorCommonResult.builder()
                    .isEnd(true)
                    .list(Collections.emptyList())
                    .build();
            return Result.success(result);
        }
        if (querySize > queryList.size()) {
            isEnd = true;
        }
        ProductVO productVO = queryList.get(queryList.size() - 1);
        Product endProduct = getById(productVO.getId());
        String sortValueByProduct = ProductSortTypeEnum.getSortValueByProduct(productSortTypeEnum, endProduct);
        CursorCommonEntity cursorCommonEntityResult = CursorCommonEntity.builder()
                .sortType(sortType)
                .querySize(querySize)
                .sortId(endProduct.getId())
                .sortValue(sortValueByProduct)
                .build();
        CursorCommonResult result = CursorCommonResult.builder()
                .isEnd(isEnd)
                .list(queryList)
                .cursorCommonEntity(cursorCommonEntityResult)
                .build();
        return Result.success(result);

    }


}
