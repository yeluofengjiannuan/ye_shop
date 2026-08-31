package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.AddressDTO;
import com.itxindeshang.pojo.entity.Address;
import com.itxindeshang.service.AddressService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {
    @Resource
    private AddressService addressService;

    /**
     * 新增地址
     */
    @PostMapping("/add")
    public Result<Address> insertAddress(@RequestBody @Validated AddressDTO addressDTO) {
        return addressService.insert(addressDTO);
    }

    /**
     * 查询地址
     */
    @GetMapping("/list")
    public Result<List<Address>> getAddressList() {
        return addressService.getAddressList();
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/delete")
    public Result<?> deleteAddress(Long addressId) {
        return addressService.deleteAddress(addressId);
    }
    /**
     * 修改地址
     */
    @PutMapping("/update")
    public Result<Address> updateAddress(@RequestBody @Validated AddressDTO addressDTO) {
        return addressService.updateAddress(addressDTO);
    }
}
