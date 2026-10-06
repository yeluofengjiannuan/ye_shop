package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.BannerDTO;
import com.itxindeshang.pojo.entity.Banner;
import com.itxindeshang.service.BannerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@Tag(name = "联播图管理")
public class BannerController {
    @Resource
    private BannerService bannerService;

    /**
     * admin 添加 banner
     */
    @PostMapping("/admin/banner/add")
    public Result<Banner> addBanner(@RequestBody @Validated BannerDTO bannerDTO) {
        return bannerService.addBanner(bannerDTO);
    }

    /**
     * 获取首页联播图列表
     */
    @GetMapping("/banner/list")
    public Result<List<Banner>> getBannerList(){
        return bannerService.getBannerList();
    }

    /**
     * admin 修改 banner
     */
    @PutMapping("/admin/banner/update")
    public Result<Banner> updateBanner(@RequestBody @Validated BannerDTO bannerDTO,Long bannerId) {
        return bannerService.updateBanner(bannerDTO,bannerId);
    }

    /**
     * admin 删除 banner
     */
    @DeleteMapping("/admin/banner/delete")
    public Result<?> deleteBanner(@RequestParam Long id) {
        return bannerService.deleteBanner(id);
    }


}
