package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.UserDetailDTO;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.pojo.vo.UserDetailVO;
import jakarta.validation.constraints.NotNull;

public interface UserService extends IService<SysUser> {
    Result<UserDetailVO> getUserDetail();

    Result<?> updateUserDetail(@NotNull UserDetailDTO userDetailDTO);
}
