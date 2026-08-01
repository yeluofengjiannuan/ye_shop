package com.itxindeshang.controller;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.UserDTO;
import com.itxindeshang.service.LoginService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    @Resource
    private LoginService loginService;

    /**
     * 用户使用账户密码登录
     *
     * @param userDTO
     * @return
     */
    @PostMapping("/account")
    public Result loginByAccount(@RequestBody @NotNull UserDTO userDTO) {
        return loginService.loginByAccount(userDTO);
    }

    /**
     * 刷新 token
     *
     * @param refreshToken
     * @return
     */
    @PostMapping("/refresh/token")
    public Result refreshToken(@RequestParam @NotBlank String refreshToken) {
        return loginService.refreshToken(refreshToken);
    }
    /**
     * 注册
     * 测试成功
     */
    @PostMapping("/register")
    public Result register(@RequestParam @NotBlank String username,
                           @RequestParam @NotBlank String password,
                           @RequestParam @NotBlank String phone) {
        return loginService.register(username, password, phone);
    }
}
