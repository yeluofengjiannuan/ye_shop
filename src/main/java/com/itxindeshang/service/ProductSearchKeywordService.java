package com.itxindeshang.service;


import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import com.baomidou.mybatisplus.extension.service.IService;



public interface ProductSearchKeywordService extends IService<ProductSearchKeyword>{
   Result getProductSearchKeywordListUser();
}
