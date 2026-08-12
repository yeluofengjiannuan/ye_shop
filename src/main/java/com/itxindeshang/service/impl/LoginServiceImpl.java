package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.JWTTokenClaimsConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.LoginInfo;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.ResultCode;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.UserMapper;
import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.dto.UserDTO;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.UserRoleEnum;
import com.itxindeshang.properties.JWTProperties;
import com.itxindeshang.security.token.JWTToken;
import com.itxindeshang.service.LoginService;
import com.itxindeshang.util.JWTUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl extends ServiceImpl<UserMapper, SysUser> implements LoginService {

    @Resource
    private UserMapper sysUserMapper;

    @Resource
    private JWTProperties jwtProperties;

    @Resource
    private CopyMapper copyMapper;

    @Override
    public Result loginByAccount(UserDTO userDTO) {
        String password = userDTO.getPassword();
        SysUser user = getSysUserByNameWithRolesAndPermissions(userDTO.getUsername());
        if (Objects.isNull(user)) {
            return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return Result.error(MessageConstant.LOGIN_ERROR);
        }
        UserInfo userInfo = copyMapper.sysUserToUserInfo(user);
        setUserInfoToRedis(user, userInfo);
        String accessToken = getAccessToken(userInfo);
        String refreshToken = getRefreshToken(userInfo);
        BaseContext.setUserInfo(userInfo);
        return Result.success(new LoginInfo(accessToken,refreshToken, userInfo));
    }

    @Override
    public Result refreshToken(String refreshToken) {
        Map<String, Object> refreshTokenMap = RedisConnector.opsForHash().entries(RedisKeyGenerator.loginRefreshToken(refreshToken));
        if (refreshTokenMap.isEmpty()) {
            return Result.error(ResultCode.REFRESH_TOKEN_EXPIRED.getCode(),MessageConstant.REFRESH_TOKEN_EXPIRED_ERROR);
        }
        String accessTokenNew = JWTUtils.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), refreshTokenMap);
        long userId = Long.parseLong(refreshTokenMap.get(JWTTokenClaimsConstant.SYS_USER_ID).toString());
        //删除旧 refreshToken,顺手更新
        RedisConnector.delete(RedisKeyGenerator.loginRefreshToken(refreshToken));
        UserInfo userInfo = UserInfo.builder().id(String.valueOf(userId)).build();
        String refreshTokenNew = getRefreshToken(userInfo);
        HashMap<String, Object> resultMap = new HashMap<>(2);
        resultMap.put(JWTToken.ACCESS_TOKEN,accessTokenNew);
        resultMap.put(JWTToken.REFRESH_TOKEN,refreshTokenNew);
        return Result.success(resultMap);
    }

    @Override
    public SysUser getSysUserByUserIdWithRolesAndPermissions(Long userId) {
        if (Objects.isNull(userId)) {
            return null;

        }
        return sysUserMapper.getSysUserByUserIdWithRolesAndPermissions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(String username, String password, String phone) {
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        if (Objects.nonNull(user)) {
            return Result.error(MessageConstant.USER_NAME_EXISTS);
        }
//        String nickname = NicknameGenerator.generateDefaultNickname();
        String nickname = username;
        String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        SysUser userNew = SysUser.builder()
                .username(username)
                .password(hashPassword)
                .phone(phone)
                .nickname(nickname)
                .build();
        //mp 插入
        boolean isSuccess = save(userNew);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        sysUserMapper.insertSysUserConnectSysRole(userNew.getId(), UserRoleEnum.ROLE_BUYER.getId());
        return Result.success();
    }

    private SysUser getSysUserByNameWithRolesAndPermissions(@NotBlank String username) {
        if (StringUtils.isBlank(username)) {
            return null;

        }
        return sysUserMapper.getSysUserByNameWithRolesAndPermissions(username);
    }
    private String getAccessToken(UserInfo userInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWTTokenClaimsConstant.SYS_USER_ID, userInfo.getId());
        return JWTUtils.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
    }
    //刷新 token 格式: UUID
    private String getRefreshToken(UserInfo userInfo) {
        String refreshToken = UUID.randomUUID().toString();
        String key = RedisKeyGenerator.loginRefreshToken(refreshToken);
        HashMap<String, Object> map = new HashMap<>(1);
        map.put(JWTTokenClaimsConstant.SYS_USER_ID, userInfo.getId());
        RedisConnector.opsForHash().putAll(key, map);
        RedisConnector.expire(key, jwtProperties.getLoginRefreshTokenTtl(), TimeUnit.DAYS);
        return refreshToken;
    }
    public void setUserInfoToRedis(SysUser user, UserInfo userInfo) {
        if (Objects.isNull(user)) {
            return;
        }
        String key = RedisKeyGenerator.loginUser(user.getId());
        //TODO:解释这个4的意义
        HashMap<String, Object> loginUserMap = new HashMap<>(4);
        loginUserMap.put(SysUser.Fields.userInfo, userInfo);
        loginUserMap.put(SysUser.Fields.isEnable, CommonStatus.ACTIVE.getNumber());
        loginUserMap.put(SysUser.Fields.sysRoleList, user.getSysRoleList());
        loginUserMap.put(SysUser.Fields.sysPermissionList, user.getSysPermissionList());
        RedisConnector.opsForHash().putAll(key, loginUserMap);
        RedisConnector.expire(key, jwtProperties.getLoginUserInfoInRedisTtl(), TimeUnit.DAYS);
        log.debug("用户信息写入Redis成功");
    }
}
