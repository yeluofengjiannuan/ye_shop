package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.entity.ProductCollection;
import jakarta.validation.constraints.NotBlank;

public interface CollectionService extends IService<ProductCollection> {
    Result<?> addCollection(@NotBlank Long productId);
}
