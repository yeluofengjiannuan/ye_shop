package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.UserDetailDTO;
import com.itxindeshang.pojo.vo.UserDetailVO;
import com.itxindeshang.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/get")
    public Result<UserDetailVO> getUserDetail() {
        return userService.getUserDetail();
    }

    /**
     * 更改用户详情
     *
     * @param userDetailDTO
     * @return
     */
    @PutMapping("/detail/update")
    public Result<?> updateUserDetail(@RequestBody @Validated UserDetailDTO userDetailDTO) {
        return userService.updateUserDetail(userDetailDTO);
    }

}
