package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.SysUser;
import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    SysUser getSysUserByNameWithRolesAndPermissions(@NotBlank String username);

    SysUser getSysUserByUserIdWithRolesAndPermissions(Long userId);

    void insertSysUserConnectSysRole(Long userId, int roleId);
}
