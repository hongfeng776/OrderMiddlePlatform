package com.orderplatform.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.common.result.ResultCode;
import com.orderplatform.inventory.entity.ProductInventory;
import com.orderplatform.inventory.mapper.ProductInventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductInventoryService extends ServiceImpl<ProductInventoryMapper, ProductInventory> {

    private static final String INVENTORY_CACHE_PREFIX = "inventory:";
    private static final String PRE_DEDUCT_PREFIX = "pre_deduct:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisDistributedLock distributedLock;

    @Autowired
    private InventoryAsyncService inventoryAsyncService;

    @Value("${inventory.lock-expire-minutes:15}")
    private int lockExpireMinutes;

    public List<ProductInventory> listAll() {
        return list(new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getStatus, 1));
    }

    public ProductInventory getByProductId(Long productId) {
        String cacheKey = INVENTORY_CACHE_PREFIX + productId;
        ProductInventory inventory = (ProductInventory) redisTemplate.opsForValue().get(cacheKey);
        if (inventory == null) {
            inventory = getOne(new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getProductId, productId));
            if (inventory != null) {
                redisTemplate.opsForValue().set(cacheKey, inventory, 30, TimeUnit.MINUTES);
            }
        }
        return inventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean preDeductStock(Long productId, Integer quantity, String orderNo) {
        String lockKey = "inventory:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            ProductInventory inventory = getByProductId(productId);
            if (inventory == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            if (inventory.getStockNum() < quantity) {
                throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
            }

            int result = baseMapper.preDeductStock(productId, quantity);
            if (result > 0) {
                String preDeductKey = PRE_DEDUCT_PREFIX + orderNo + ":" + productId;
                redisTemplate.opsForValue().set(preDeductKey, quantity, lockExpireMinutes, TimeUnit.MINUTES);
                
                String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                redisTemplate.delete(cacheKey);

                inventoryAsyncService.recordInventoryChangeLog(
                        productId, inventory.getProductName(),
                        inventory.getStockNum(), inventory.getStockNum() - quantity,
                        orderNo, "预扣"
                );
                
                inventoryAsyncService.sendLowStockAlert(productId, inventory.getProductName(), inventory.getStockNum() - quantity);
                
                log.info("预扣库存成功: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
                return true;
            }
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirmDeductStock(Long productId, Integer quantity, String orderNo) {
        String lockKey = "inventory:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            String preDeductKey = PRE_DEDUCT_PREFIX + orderNo + ":" + productId;
            Object preDeductQuantity = redisTemplate.opsForValue().get(preDeductKey);
            
            if (preDeductQuantity == null) {
                log.warn("预扣库存记录不存在，可能已超时回滚: orderNo={}, productId={}", orderNo, productId);
                return false;
            }

            int result = baseMapper.confirmDeductStock(productId, quantity);
            if (result > 0) {
                redisTemplate.delete(preDeductKey);
                
                String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                redisTemplate.delete(cacheKey);

                ProductInventory inventory = getByProductId(productId);
                inventoryAsyncService.recordInventoryChangeLog(
                        productId, inventory.getProductName(),
                        inventory.getStockNum() + quantity, inventory.getStockNum(),
                        orderNo, "确认扣减"
                );
                
                inventoryAsyncService.sendStockDeductionNotification(productId, inventory.getProductName(), quantity, orderNo);
                
                log.info("确认扣减库存成功: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
                return true;
            }
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackStock(Long productId, Integer quantity, String orderNo) {
        String lockKey = "inventory:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            String preDeductKey = PRE_DEDUCT_PREFIX + orderNo + ":" + productId;
            Object preDeductQuantity = redisTemplate.opsForValue().get(preDeductKey);
            
            if (preDeductQuantity == null) {
                log.warn("预扣库存记录不存在，无需回滚: orderNo={}, productId={}", orderNo, productId);
                return true;
            }

            int result = baseMapper.rollbackStock(productId, quantity);
            if (result > 0) {
                redisTemplate.delete(preDeductKey);
                
                String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                redisTemplate.delete(cacheKey);

                ProductInventory inventory = getByProductId(productId);
                inventoryAsyncService.recordInventoryChangeLog(
                        productId, inventory.getProductName(),
                        inventory.getStockNum() - quantity, inventory.getStockNum(),
                        orderNo, "回滚"
                );
                
                log.info("库存回滚成功: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
                return true;
            }
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long productId, Integer quantity) {
        ProductInventory inventory = getByProductId(productId);
        if (inventory == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (inventory.getStockNum() < quantity) {
            throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
        }
        
        int result = baseMapper.lockStock(productId, quantity);
        return result > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unlockStock(Long productId, Integer quantity) {
        int result = baseMapper.unlockStock(productId, quantity);
        return result > 0;
    }
}
