package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.SimpleCursorCommonEntity;
import com.itxindeshang.common.result.SimpleCursorCommonResult;
import com.itxindeshang.pojo.dto.UserDetailDTO;
import com.itxindeshang.pojo.vo.UserDetailVO;
import com.itxindeshang.service.CollectionService;
import com.itxindeshang.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private CollectionService collectionService;

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/get")
    public Result<UserDetailVO> getUserDetail() {
        return userService.getUserDetail();
    }

    /**
     * 更改用户详情
     */
    @PutMapping("/detail/update")
    public Result<?> updateUserDetail(@RequestBody @Validated UserDetailDTO userDetailDTO) {
        return userService.updateUserDetail(userDetailDTO);
    }

    /**
     * 新增收藏
     *
     * @param productId
     * @return
     */
    @PostMapping("/collect/add")
    public Result<?> addCollection(@RequestParam Long productId) {
        return collectionService.addCollection(productId);
    }

    /**
     * 删除收藏
     */
    @DeleteMapping("/collect/delete")
    public Result<?> deleteCollection(@RequestParam List<Long> productIds) {
        return collectionService.deleteCollection(productIds);
    }

    /**
     * 获取用户收藏商品列表
     * @param simpleCursorCommonEntity 简单查询请求参数
     * @return 简单商品封装列表
     */
    @GetMapping("/collect/list")
    public Result<SimpleCursorCommonResult>getCollectionList(@Valid SimpleCursorCommonEntity simpleCursorCommonEntity) {
        return collectionService.getCollectionList(simpleCursorCommonEntity);
    }
}
