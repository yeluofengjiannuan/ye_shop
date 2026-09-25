package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.SimpleCursorCommonEntity;
import com.itxindeshang.common.result.SimpleCursorCommonResult;
import com.itxindeshang.pojo.entity.ProductCollection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface CollectionService extends IService<ProductCollection> {
    Result<?> addCollection(@NotBlank Long productId);

    Result<?> deleteCollection(List<Long> productIds);

    Result<SimpleCursorCommonResult> getCollectionList(SimpleCursorCommonEntity simpleCursorCommonEntity);
}
