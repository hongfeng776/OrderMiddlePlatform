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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductInventoryService extends ServiceImpl<ProductInventoryMapper, ProductInventory> {

    private static final String INVENTORY_CACHE_PREFIX = "inventory:";
    private static final String PRE_DEDUCT_PREFIX = "pre_deduct:";
    private static final String STOCK_VERSION_PREFIX = "stock_version:";

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
                redisTemplate.opsForValue().set(cacheKey, inventory, 5, TimeUnit.MINUTES);
            }
        }
        return inventory;
    }

    private ProductInventory getByProductIdFromDB(Long productId) {
        return getOne(new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getProductId, productId));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean preDeductStock(Long productId, Integer quantity, String orderNo) {
        String lockKey = "inventory_lock:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            ProductInventory inventory = getByProductIdFromDB(productId);
            if (inventory == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            
            if (inventory.getStockNum() < quantity) {
                log.warn("库存不足: productId={}, stock={}, required={}", productId, inventory.getStockNum(), quantity);
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
                
                log.info("预扣库存成功: productId={}, quantity={}, orderNo={}, before={}, after={}", 
                        productId, quantity, orderNo, inventory.getStockNum(), inventory.getStockNum() - quantity);
                return true;
            }
            log.error("预扣库存失败: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirmDeductStock(Long productId, Integer quantity, String orderNo) {
        String lockKey = "inventory_lock:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
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

            ProductInventory beforeInventory = getByProductIdFromDB(productId);
            
            int result = baseMapper.confirmDeductStock(productId, quantity);
            if (result > 0) {
                redisTemplate.delete(preDeductKey);
                
                String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                redisTemplate.delete(cacheKey);

                inventoryAsyncService.recordInventoryChangeLog(
                        productId, beforeInventory.getProductName(),
                        beforeInventory.getLockStock(), beforeInventory.getLockStock() - quantity,
                        orderNo, "确认扣减"
                );
                
                inventoryAsyncService.sendStockDeductionNotification(productId, beforeInventory.getProductName(), quantity, orderNo);
                
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
        String lockKey = "inventory_lock:" + productId;
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
        if (!locked) {
            log.warn("回滚库存获取锁失败: productId={}, orderNo={}", productId, orderNo);
            return false;
        }

        try {
            String preDeductKey = PRE_DEDUCT_PREFIX + orderNo + ":" + productId;
            Object preDeductQuantity = redisTemplate.opsForValue().get(preDeductKey);
            
            if (preDeductQuantity == null) {
                log.warn("预扣库存记录不存在，无需回滚: orderNo={}, productId={}", orderNo, productId);
                return true;
            }

            ProductInventory beforeInventory = getByProductIdFromDB(productId);

            int result = baseMapper.rollbackStock(productId, quantity);
            if (result > 0) {
                redisTemplate.delete(preDeductKey);
                
                String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                redisTemplate.delete(cacheKey);

                inventoryAsyncService.recordInventoryChangeLog(
                        productId, beforeInventory.getProductName(),
                        beforeInventory.getStockNum(), beforeInventory.getStockNum() + quantity,
                        orderNo, "回滚"
                );
                
                log.info("库存回滚成功: productId={}, quantity={}, orderNo={}, before={}, after={}",
                        productId, quantity, orderNo, beforeInventory.getStockNum(), beforeInventory.getStockNum() + quantity);
                return true;
            }
            log.error("库存回滚失败: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackStockByOrderNo(String orderNo) {
        log.info("开始回滚订单库存: orderNo={}", orderNo);
        String pattern = PRE_DEDUCT_PREFIX + orderNo + ":*";
        List<String> keys = new ArrayList<>();
        
        try {
            keys.addAll(redisTemplate.keys(pattern));
        } catch (Exception e) {
            log.error("获取预扣库存key失败: orderNo={}", orderNo, e);
        }

        if (keys.isEmpty()) {
            log.info("没有需要回滚的预扣库存: orderNo={}", orderNo);
            return true;
        }

        boolean allSuccess = true;
        for (String key : keys) {
            try {
                Long productId = Long.parseLong(key.substring(key.lastIndexOf(":") + 1));
                Integer quantity = (Integer) redisTemplate.opsForValue().get(key);
                
                if (quantity != null) {
                    String lockKey = "inventory_lock:" + productId;
                    boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
                    if (!locked) {
                        log.warn("回滚库存获取锁失败,跳过: productId={}, orderNo={}", productId, orderNo);
                        allSuccess = false;
                        continue;
                    }
                    
                    try {
                        ProductInventory beforeInventory = getByProductIdFromDB(productId);
                        int result = baseMapper.rollbackStock(productId, quantity);
                        if (result > 0) {
                            redisTemplate.delete(key);
                            
                            String cacheKey = INVENTORY_CACHE_PREFIX + productId;
                            redisTemplate.delete(cacheKey);
                            
                            inventoryAsyncService.recordInventoryChangeLog(
                                    productId, beforeInventory.getProductName(),
                                    beforeInventory.getStockNum(), beforeInventory.getStockNum() + quantity,
                                    orderNo, "超时回滚"
                            );
                            log.info("超时回滚库存成功: productId={}, quantity={}, orderNo={}", productId, quantity, orderNo);
                        }
                    } finally {
                        distributedLock.unlock(lockKey);
                    }
                }
            } catch (Exception e) {
                log.error("回滚单个商品库存失败: key={}", key, e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long productId, Integer quantity) {
        ProductInventory inventory = getByProductIdFromDB(productId);
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
