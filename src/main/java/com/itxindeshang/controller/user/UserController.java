package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.UserDetailDTO;
import com.itxindeshang.pojo.vo.UserDetailVO;
import com.itxindeshang.service.CollectionService;
import com.itxindeshang.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
