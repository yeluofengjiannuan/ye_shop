package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.AddressDTO;
import com.itxindeshang.pojo.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {
    Result<Address> insert(AddressDTO addressDTO);

    Result<List<Address>> getAddressList();

    Result<?> deleteAddress(Long addressId);

    Result<Address> updateAddress(AddressDTO addressDTO);
}
