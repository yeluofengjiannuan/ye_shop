package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.mapper.UserMapper;
import com.itxindeshang.pojo.dto.UserDetailDTO;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.pojo.vo.UserDetailVO;
import com.itxindeshang.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Resource
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {
    @Resource
    private CopyMapper copyMapper;

    /**
     * 查询用户信息详情
     * @return
     */
    @Override
    public Result<UserDetailVO> getUserDetail() {
        String userId = BaseContext.getUserId();
        SysUser user = lambdaQuery().eq(SysUser::getId, userId).one();
        UserDetailVO userDetailVO = copyMapper.sysUserToUserDetailVO(user);
        return Result.success(userDetailVO);
    }
    /**
     * 修改用户详情
     */
    @Override
    public Result updateUserDetail(UserDetailDTO userDetailDTO) {
        String userId = BaseContext.getUserId();
        //更新数据库
        boolean isSuccess = lambdaUpdate().eq(SysUser::getId, userId).set(SysUser::getNickname, userDetailDTO.getNickname())
                .set(SysUser::getAvatar, userDetailDTO.getAvatar()).set(SysUser::getPhone, userDetailDTO.getPhone()).update();
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        return Result.success();
    }
}
