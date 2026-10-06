package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.BannerMapper;
import com.itxindeshang.pojo.dto.BannerDTO;
import com.itxindeshang.pojo.entity.Banner;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.service.BannerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
    @Resource
    private CopyMapper copyMapper;

    /**
     * 管理员新增轮播图
     */
    @Override
    public Result<Banner> addBanner(BannerDTO bannerDTO) {
        Banner banner = copyMapper.bannerDTOToBanner(bannerDTO);
        save(banner);
        return Result.success(banner);
    }

    /**
     * 查询轮播图集合
     */
    @Override
    public Result<List<Banner>> getBannerList() {
        String bannerKey = RedisKeyGenerator.banner();
        List<Object> bannerList = RedisConnector.opsForList().range(bannerKey, 0, -1);

        if (Objects.isNull(bannerList) || bannerList.isEmpty()) {
            List<Banner> banners = lambdaQuery()
                    .orderByAsc(Banner::getSort)
                    .eq(Banner::getStatus, CommonStatus.ACTIVE)
                    .list();
            RedisConnector.delete(bannerKey);
            RedisConnector.opsForList().rightPushAll(bannerKey, banners.toArray());
            return Result.success(banners);
        }
//        Collections.reverse(bannerList);
        List<Banner> resultList = bannerList.stream().map(object -> (Banner) object).toList();
        return Result.success(resultList);
    }
}
