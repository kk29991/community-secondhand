package com.mall.controller;

import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.AddressDTO;
import com.mall.entity.Address;
import com.mall.mapper.AddressMapper;
import com.mall.common.BusinessException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址管理 (需登录)
 */
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressMapper addressMapper;

    /** 我的地址列表, 默认地址优先 */
    @GetMapping("/list")
    public Result<List<Address>> list() {
        Long userId = UserContext.requireId();
        return Result.success(addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime)));
    }

    /** 地址详情 */
    @GetMapping("/{id}")
    public Result<Address> detail(@PathVariable Long id) {
        return Result.success(requireOwned(id));
    }

    /** 新增地址 */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Address> create(@Valid @RequestBody AddressDTO dto) {
        Long userId = UserContext.requireId();
        Address address = new Address();
        copy(dto, address);
        address.setUserId(userId);

        long exists = addressMapper.selectCount(Wrappers.<Address>lambdaQuery()
                .eq(Address::getUserId, userId));
        // 首个地址自动设为默认
        if (exists == 0) {
            address.setIsDefault(1);
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return Result.success("地址已保存", address);
    }

    /** 修改地址 */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Address> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        Address address = requireOwned(id);
        copy(dto, address);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
            address.setIsDefault(1);
        }
        addressMapper.updateById(address);
        return Result.success("地址已更新", address);
    }

    /** 设为默认地址 */
    @PutMapping("/{id}/default")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setDefault(@PathVariable Long id) {
        Address address = requireOwned(id);
        clearDefault(address.getUserId());
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return Result.success("已设为默认地址", null);
    }

    /** 删除地址 */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        Address address = requireOwned(id);
        addressMapper.deleteById(id);
        // 若删除的是默认地址, 顺延一个
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            List<Address> rest = addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                    .eq(Address::getUserId, address.getUserId())
                    .orderByDesc(Address::getCreateTime));
            if (!rest.isEmpty()) {
                Address first = rest.get(0);
                first.setIsDefault(1);
                addressMapper.updateById(first);
            }
        }
        return Result.success("地址已删除", null);
    }

    private void clearDefault(Long userId) {
        Address reset = new Address();
        reset.setIsDefault(0);
        addressMapper.update(reset, Wrappers.<Address>lambdaUpdate().eq(Address::getUserId, userId));
    }

    private Address requireOwned(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw BusinessException.notFound("地址不存在");
        }
        if (!UserContext.requireId().equals(address.getUserId())) {
            throw BusinessException.forbidden("无权操作该地址");
        }
        return address;
    }

    private void copy(AddressDTO dto, Address address) {
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
    }
}
