package com.orderplatform.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.result.ResultCode;
import com.orderplatform.inventory.entity.ProductInventory;
import com.orderplatform.inventory.mapper.ProductInventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductInventoryService extends ServiceImpl<ProductInventoryMapper, ProductInventory> {

    public List<ProductInventory> listAll() {
        return list(new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getStatus, 1));
    }

    public ProductInventory getByProductId(Long productId) {
        return getOne(new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getProductId, productId));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long productId, Integer count) {
        ProductInventory inventory = getByProductId(productId);
        if (inventory == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (inventory.getStockNum() < count) {
            throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
        }
        
        int result = baseMapper.lockStock(productId, count);
        return result > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unlockStock(Long productId, Integer count) {
        int result = baseMapper.unlockStock(productId, count);
        return result > 0;
    }
}
