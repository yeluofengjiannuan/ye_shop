package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.pojo.vo.UserDetailVO;

public interface UserService extends IService<SysUser> {
    Result<UserDetailVO> getUserDetail();
}
