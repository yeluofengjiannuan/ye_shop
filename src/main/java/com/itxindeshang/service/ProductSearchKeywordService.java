package com.itxindeshang.service;


import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ProductSearchKeywordService extends IService<ProductSearchKeyword>{
   Result<List<String>> getProductSearchKeywordListUser();

    Result<List<ProductSearchKeyword>> getProductSearchKeywordListAdmin();
}
