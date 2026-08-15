package com.itxindeshang.security.realm;

import com.itxindeshang.common.constant.JWTTokenClaimsConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.exception.InvalidCredentialsException;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.entity.SysPermission;
import com.itxindeshang.pojo.entity.SysRole;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.security.token.JWTToken;
import com.itxindeshang.service.impl.LoginServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Setter
public class CustomRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(CustomRealm.class);

    @Resource
    private LoginServiceImpl loginServiceImpl;

    @Resource
    private CopyMapper copyMapper;

    /**
     * token类型必须是JWTToken
     * @param token the token being submitted for authentication.
     * @return
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUser user =(SysUser) principals.getPrimaryPrincipal();
        if (Objects.isNull(user)) {
            log.error("授权失败：用户不存在");
            throw new UnknownAccountException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 提取角色和权限
        Set<String> roleNames = user.getSysRoleList().stream()
                .map(SysRole::getRoleName)
                .collect(Collectors.toSet());
        Set<String> permNames = user.getSysPermissionList().stream()
                .map(SysPermission::getPermName)
                .collect(Collectors.toSet());

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.setRoles(roleNames);
        authorizationInfo.setStringPermissions(permNames);

        // 注意：doGetAuthorizationInfo 只有在鉴权（@RequiresRoles）时才会被调用
        // 对于普通接口，认证通过后并不会自动调用这里，所以 BaseContext 必须在认证阶段（doGetAuthenticationInfo）或 Filter 中设置
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        JWTToken jwtToken = (JWTToken) token;
        String tokenChecked = (String) jwtToken.getCredentials();
        String userId = (String) jwtToken.getPrincipal();
        Claims claims = jwtToken.getClaims(); // 直接获取 Filter 解析好的 Claims
        // 验证 userId 是否一致（双重校验）
        //TODO 此处可以优化为缓存,记得换常量
        String jwtUserId = claims.get(JWTTokenClaimsConstant.SYS_USER_ID).toString();
        if (!userId.equals(jwtUserId)) {
            log.error("Token篡改：传入userId={}，JWT解析userId={}", userId, jwtUserId);
              throw new InvalidCredentialsException(MessageConstant.TOKEN_INVALID);
        }
        //查询用户角色和权限，这里是查redis的，如果redis没有就查sql更新redis
        // 查询用户（带角色和权限）
        Map<String, Object> userMap = RedisConnector.opsForHash().entries(RedisKeyGenerator.loginUser(Long.parseLong(userId)));
        if (userMap.isEmpty()) {
            SysUser user = loginServiceImpl.getSysUserByUserIdWithRolesAndPermissions(Long.valueOf(userId));
            if (Objects.isNull(user)){
                throw new UnknownAccountException(MessageConstant.USER_NOT_LOGIN);
            }
            UserInfo userInfo = copyMapper.sysUserToUserInfo(user);
            //TODO:这里就是没有检验导致出问题
            loginServiceImpl.setUserInfoToRedis(user, userInfo);
            user.setUserInfo(userInfo);
            return new SimpleAuthenticationInfo(user, token, this.getName());
        }
        //TODO:为什么查到能出现isNotEnable
        if (!userMap.get(SysUser.Fields.isEnable).equals(CommonStatus.ACTIVE.getNumber())) {
            throw new DisabledAccountException(MessageConstant.ACCOUNT_LOCKED);

        }

        List<SysRole> sysRoleList = (List<SysRole>) userMap.get(SysUser.Fields.sysRoleList);
        List<SysPermission> sysPermissionList = (List<SysPermission>) userMap.get(SysUser.Fields.sysPermissionList);
        UserInfo userInfo = (UserInfo) userMap.get(SysUser.Fields.userInfo);


        if (Objects.isNull(sysRoleList) || Objects.isNull(sysPermissionList) || Objects.isNull(userInfo)) {
            throw new UnknownAccountException(MessageConstant.USER_NOT_LOGIN);

        }
        SysUser user = SysUser.builder()
                .id(Long.valueOf(userId))
                .sysRoleList(sysRoleList)
                .sysPermissionList(sysPermissionList)
                .userInfo(userInfo)
                .build();
        //TODO：检验是否有问题
        return new SimpleAuthenticationInfo(user, tokenChecked, this.getName());
    }
}
