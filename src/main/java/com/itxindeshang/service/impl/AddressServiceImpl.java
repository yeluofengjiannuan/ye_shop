package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.mapper.AddressMapper;
import com.itxindeshang.pojo.dto.AddressDTO;
import com.itxindeshang.pojo.entity.Address;
import com.itxindeshang.pojo.enums.CommonDefault;
import com.itxindeshang.service.AddressService;
import jakarta.annotation.Resource;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Resource
    private AddressMapper addressMapper;
    
    @Resource
    private CopyMapper copyMapper;
    
    /**
     * 新增地址
     */
    @Override
    public Result<Address> insert(AddressDTO addressDTO) {
        String userId = BaseContext.getUserId();
        Address address = copyMapper.addressDTOToAddress(addressDTO);
        address.setUserId(Long.valueOf(userId));
        //代理对象，确保事务能触发
        AddressServiceImpl addressService = (AddressServiceImpl) AopContext.currentProxy();
        addressService.makeOnlyHaveOneDefault(addressDTO, userId);
        boolean isSuccess = save(address);
        if (!isSuccess) {
            return Result.error(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        return Result.success(address);
    }

    /**
     * 查询地址列表
     */
    @Override
    public Result<List<Address>> getAddressList() {
        String userId = BaseContext.getUserId();
        List<Address> list = lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getUpdateTime)
                .list();
        return Result.success(list);
    }
    /**
     * 保证只有一个默认地址
     * @param userId 用户id
     */
    @Transactional(rollbackFor = Exception.class)
    void makeOnlyHaveOneDefault(AddressDTO addressDTO, String userId) {
        // 1. 只有前端要求设为默认时，才去处理
        //这样编写可以防止空指针异常
        if (addressDTO.getIsDefault() == CommonDefault.DEFAULT) {
            //直接用数据库 UPDATE 把该用户的所有地址取消默认
            lambdaUpdate()
                    .eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, CommonDefault.NO_DEFAULT.getNumber())
                    .update();
        }
    }
    /**
     * 删除地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteAddress(Long addressId) {
        String userId = BaseContext.getUserId();

        // 防止越权删除
        Address address = lambdaQuery()
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, Long.valueOf(userId))
                .one();

        if (address == null) {
            return Result.error(MessageConstant.DATA_ERROR);
        }

        //如果删除的是默认地址最新的一条非默认地址提升为默认
        if (address.getIsDefault() == CommonDefault.DEFAULT) {
            // 找到该用户最新创建的一条非默认地址
            Address newDefault = lambdaQuery()
                    .eq(Address::getUserId, Long.valueOf(userId))
                    .ne(Address::getId, addressId) // 排除掉当前要删除的
                    .orderByDesc(Address::getUpdateTime)
                    .last("LIMIT 1")
                    .one();

            if (newDefault != null) {
                // 更新新默认地址（复用你的事务代理，保证绝对安全）
                AddressServiceImpl proxy = (AddressServiceImpl) AopContext.currentProxy();
                proxy.updateDefaultAddress(newDefault.getId());
            }
        }

        //物理删除当前地址
        boolean isSuccess = removeById(addressId);
        if (!isSuccess) {
            throw new RuntimeException(MessageConstant.SQL_MESSAGE_DELETE_ERROR);
        }

        return Result.success();
    }

    /**
     * 将指定地址设为默认
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDefaultAddress(Long addressId) {
        // 1. 先把该用户的所有地址取消默认
        lambdaUpdate()
                .eq(Address::getUserId, BaseContext.getUserId())
                .set(Address::getIsDefault, CommonDefault.NO_DEFAULT.getNumber())
                .update();
        // 2. 把目标地址设为默认
        lambdaUpdate()
                .eq(Address::getId, addressId)
                .set(Address::getIsDefault, CommonDefault.DEFAULT.getNumber())
                .update();
    }

}
