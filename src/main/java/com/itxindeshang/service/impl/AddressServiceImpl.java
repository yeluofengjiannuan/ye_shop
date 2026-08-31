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
import org.apache.commons.collections4.CollectionUtils;
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
     * @param addressDTO
     * @return
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
}
