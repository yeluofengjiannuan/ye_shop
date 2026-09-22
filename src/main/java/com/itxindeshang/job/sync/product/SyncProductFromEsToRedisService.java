package com.itxindeshang.job.sync.product;

public interface SyncProductFromEsToRedisService {


    /**
     * 同步热门商品缓存
     */
    void syncHotProductCache();
}
