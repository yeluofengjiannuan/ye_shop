package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.BannerDTO;
import com.itxindeshang.pojo.entity.Banner;

import java.util.List;

public interface BannerService extends IService<Banner> {
    Result<Banner> addBanner(BannerDTO bannerDTO);

    Result<List<Banner>> getBannerList();

    Result<Banner> updateBanner(BannerDTO bannerDTO,Long bannerId);

    Result<?> deleteBanner(Long id);


}
