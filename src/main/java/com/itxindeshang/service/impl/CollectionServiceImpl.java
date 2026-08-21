package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.mapper.CollectionMapper;
import com.itxindeshang.pojo.entity.ProductCollection;
import com.itxindeshang.service.CollectionService;
import org.springframework.stereotype.Service;

@Service
public class CollectionServiceImpl  extends ServiceImpl<CollectionMapper, ProductCollection> implements CollectionService {
}
