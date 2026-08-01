package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.UserDTO;
import com.itxindeshang.pojo.entity.SysUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface LoginService extends IService<SysUser> {
    Result loginByAccount(@NotNull UserDTO userDTO);

    Result refreshToken(@NotBlank String refreshToken);

    SysUser getSysUserByUserIdWithRolesAndPermissions(Long userId);

    Result register(@NotBlank String username, @NotBlank String password, @NotBlank String phone);
}
