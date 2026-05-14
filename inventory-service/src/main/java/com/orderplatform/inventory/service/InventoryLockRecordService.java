package com.orderplatform.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.common.result.ResultCode;
import com.orderplatform.inventory.entity.InventoryLockRecord;
import com.orderplatform.inventory.entity.ProductInventory;
import com.orderplatform.inventory.enums.InventoryLockStatusEnum;
import com.orderplatform.inventory.enums.InventoryLockTypeEnum;
import com.orderplatform.inventory.enums.InventoryReleaseTypeEnum;
import com.orderplatform.inventory.mapper.InventoryLockRecordMapper;
import com.orderplatform.inventory.mapper.ProductInventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InventoryLockRecordService extends ServiceImpl<InventoryLockRecordMapper, InventoryLockRecord> {

    private static final String INVENTORY_STOCK_KEY = "inventory:stock:";
    private static final String INVENTORY_LOCK_KEY = "inventory_lock:";
    private static final String ABNORMAL_LOCK_REMARK = "【异常锁定】";

    @Autowired
    private ProductInventoryMapper productInventoryMapper;

    @Resource
    private RedisDistributedLock distributedLock;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${inventory.lock-expire-minutes:30}")
    private int lockExpireMinutes;

    @Value("${inventory.enable-anti-oversell:true}")
    private boolean enableAntiOversell;

    public Page<InventoryLockRecord> listLockRecords(int page, int size, Long productId, String orderNo, Integer lockStatus) {
        LambdaQueryWrapper<InventoryLockRecord> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            wrapper.eq(InventoryLockRecord::getProductId, productId);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.eq(InventoryLockRecord::getOrderNo, orderNo);
        }
        if (lockStatus != null) {
            wrapper.eq(InventoryLockRecord::getLockStatus, lockStatus);
        }
        wrapper.orderByDesc(InventoryLockRecord::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryLockRecord createLockRecord(Long productId, Integer quantity, String orderNo, Long userId, Integer lockType) {
        String lockKey = INVENTORY_LOCK_KEY + productId;
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            ProductInventory inventory = productInventoryMapper.selectOne(
                    new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getProductId, productId)
            );
            if (inventory == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }

            if (inventory.getStatus() != 1) {
                throw new BusinessException("商品已下架，无法购买");
            }

            if (enableAntiOversell) {
                int availableStock = getAvailableStockWithCache(inventory);
                if (availableStock < quantity) {
                    log.warn("库存不足，超卖拦截: productId={}, available={}, required={}", productId, availableStock, quantity);
                    throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
                }
            } else {
                int availableStock = inventory.getStockNum() - inventory.getLockStock();
                if (availableStock < quantity) {
                    log.warn("库存不足: productId={}, available={}, required={}", productId, availableStock, quantity);
                    throw new BusinessException(ResultCode.INVENTORY_NOT_ENOUGH);
                }
            }

            int result = productInventoryMapper.lockStock(productId, quantity);
            if (result <= 0) {
                log.error("锁定库存失败: productId={}, quantity={}", productId, quantity);
                throw new BusinessException("锁定库存失败");
            }

            if (enableAntiOversell) {
                updateStockCache(inventory);
            }

            InventoryLockRecord lockRecord = new InventoryLockRecord();
            lockRecord.setLockNo("LOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
            lockRecord.setProductId(productId);
            lockRecord.setProductName(inventory.getProductName());
            lockRecord.setOrderNo(orderNo);
            lockRecord.setUserId(userId);
            lockRecord.setLockQuantity(quantity);
            lockRecord.setLockStatus(InventoryLockStatusEnum.LOCKING.getCode());
            lockRecord.setLockType(lockType);
            lockRecord.setLockExpireTime(LocalDateTime.now().plusMinutes(lockExpireMinutes));
            save(lockRecord);

            log.info("创建库存锁定记录成功: lockNo={}, productId={}, quantity={}, orderNo={}",
                    lockRecord.getLockNo(), productId, quantity, orderNo);
            return lockRecord;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private int getAvailableStockWithCache(ProductInventory inventory) {
        String cacheKey = INVENTORY_STOCK_KEY + inventory.getProductId();
        Integer cachedStock = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStock != null) {
            return cachedStock;
        }
        int availableStock = inventory.getStockNum() - inventory.getLockStock();
        redisTemplate.opsForValue().set(cacheKey, availableStock, 5, TimeUnit.MINUTES);
        return availableStock;
    }

    private void updateStockCache(ProductInventory inventory) {
        String cacheKey = INVENTORY_STOCK_KEY + inventory.getProductId();
        ProductInventory latestInventory = productInventoryMapper.selectOne(
                new LambdaQueryWrapper<ProductInventory>().eq(ProductInventory::getProductId, inventory.getProductId())
        );
        if (latestInventory != null) {
            int availableStock = latestInventory.getStockNum() - latestInventory.getLockStock();
            redisTemplate.opsForValue().set(cacheKey, availableStock, 5, TimeUnit.MINUTES);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirmLockByOrderNo(String orderNo) {
        List<InventoryLockRecord> lockRecords = baseMapper.selectLockingRecordsByOrderNo(orderNo);
        if (lockRecords.isEmpty()) {
            log.info("没有需要确认的库存锁定记录: orderNo={}", orderNo);
            return true;
        }

        boolean allSuccess = true;
        for (InventoryLockRecord record : lockRecords) {
            String lockKey = INVENTORY_LOCK_KEY + record.getProductId();
            boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("确认库存锁定获取锁失败，跳过: lockNo={}, productId={}", record.getLockNo(), record.getProductId());
                allSuccess = false;
                continue;
            }

            try {
                int result = productInventoryMapper.confirmDeductStock(record.getProductId(), record.getLockQuantity());
                if (result > 0) {
                    baseMapper.confirmLock(record.getId(), LocalDateTime.now());
                    if (enableAntiOversell) {
                        clearStockCache(record.getProductId());
                    }
                    log.info("确认库存锁定成功: lockNo={}, productId={}, quantity={}",
                            record.getLockNo(), record.getProductId(), record.getLockQuantity());
                } else {
                    log.warn("确认库存锁定失败: lockNo={}, productId={}", record.getLockNo(), record.getProductId());
                    allSuccess = false;
                }
            } finally {
                distributedLock.unlock(lockKey);
            }
        }
        return allSuccess;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean releaseLockByOrderNo(String orderNo, Integer releaseType, Long operatorId, String remark) {
        List<InventoryLockRecord> lockRecords = baseMapper.selectLockingRecordsByOrderNo(orderNo);
        if (lockRecords.isEmpty()) {
            log.info("没有需要释放的库存锁定记录: orderNo={}", orderNo);
            return true;
        }

        boolean allSuccess = true;
        for (InventoryLockRecord record : lockRecords) {
            String lockKey = INVENTORY_LOCK_KEY + record.getProductId();
            boolean locked = distributedLock.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("释放库存获取锁失败，跳过: lockNo={}, productId={}", record.getLockNo(), record.getProductId());
                allSuccess = false;
                continue;
            }

            try {
                int result = productInventoryMapper.rollbackStock(record.getProductId(), record.getLockQuantity());
                if (result > 0) {
                    baseMapper.releaseLock(record.getId(), LocalDateTime.now(), releaseType, operatorId, remark);
                    if (enableAntiOversell) {
                        clearStockCache(record.getProductId());
                    }
                    log.info("释放库存锁定成功: lockNo={}, productId={}, quantity={}, releaseType={}",
                            record.getLockNo(), record.getProductId(), record.getLockQuantity(), releaseType);
                } else {
                    log.warn("释放库存锁定失败: lockNo={}, productId={}", record.getLockNo(), record.getProductId());
                    allSuccess = false;
                }
            } finally {
                distributedLock.unlock(lockKey);
            }
        }
        return allSuccess;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean manualReleaseLock(Long lockRecordId, Long operatorId, String remark) {
        InventoryLockRecord record = getById(lockRecordId);
        if (record == null) {
            throw new BusinessException("库存锁定记录不存在");
        }
        if (!InventoryLockStatusEnum.LOCKING.getCode().equals(record.getLockStatus())) {
            throw new BusinessException("该锁定记录已处理");
        }

        String lockKey = INVENTORY_LOCK_KEY + record.getProductId();
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            int result = productInventoryMapper.rollbackStock(record.getProductId(), record.getLockQuantity());
            if (result > 0) {
                baseMapper.releaseLock(lockRecordId, LocalDateTime.now(),
                        InventoryReleaseTypeEnum.MANUAL_RELEASE.getCode(), operatorId, remark);
                if (enableAntiOversell) {
                    clearStockCache(record.getProductId());
                }
                log.info("手动释放库存锁定成功: lockNo={}, productId={}, quantity={}, operatorId={}",
                        record.getLockNo(), record.getProductId(), record.getLockQuantity(), operatorId);
                return true;
            }
            log.error("手动释放库存锁定失败: lockNo={}, productId={}", record.getLockNo(), record.getProductId());
            return false;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean forceReleaseLock(Long lockRecordId, Long operatorId, String remark) {
        InventoryLockRecord record = getById(lockRecordId);
        if (record == null) {
            throw new BusinessException("库存锁定记录不存在");
        }
        if (!InventoryLockStatusEnum.LOCKING.getCode().equals(record.getLockStatus())) {
            throw new BusinessException("该锁定记录已处理");
        }

        String lockKey = INVENTORY_LOCK_KEY + record.getProductId();
        boolean locked = distributedLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
        if (!locked) {
            log.warn("强制解锁获取锁失败，继续执行: lockNo={}", record.getLockNo());
        }

        try {
            int result = productInventoryMapper.rollbackStock(record.getProductId(), record.getLockQuantity());
            String finalRemark = ABNORMAL_LOCK_REMARK + (remark != null ? remark : "管理员强制解锁");
            if (result > 0) {
                baseMapper.releaseLock(lockRecordId, LocalDateTime.now(),
                        InventoryReleaseTypeEnum.MANUAL_RELEASE.getCode(), operatorId, finalRemark);
                if (enableAntiOversell) {
                    clearStockCache(record.getProductId());
                }
                log.warn("强制释放异常库存锁定成功: lockNo={}, productId={}, quantity={}, operatorId={}",
                        record.getLockNo(), record.getProductId(), record.getLockQuantity(), operatorId);
                return true;
            } else {
                log.warn("强制解锁回滚库存返回0，标记为异常释放: lockNo={}", record.getLockNo());
                baseMapper.releaseLock(lockRecordId, LocalDateTime.now(),
                        InventoryReleaseTypeEnum.MANUAL_RELEASE.getCode(), operatorId, finalRemark + " - 库存回滚异常");
                return true;
            }
        } catch (Exception e) {
            log.error("强制释放库存锁定异常: lockNo={}", record.getLockNo(), e);
            String finalRemark = ABNORMAL_LOCK_REMARK + (remark != null ? remark : "管理员强制解锁") + " - 异常处理";
            baseMapper.releaseLock(lockRecordId, LocalDateTime.now(),
                    InventoryReleaseTypeEnum.MANUAL_RELEASE.getCode(), operatorId, finalRemark);
            return true;
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int releaseExpiredLocks() {
        List<InventoryLockRecord> expiredRecords = baseMapper.selectExpiredLockingRecords(LocalDateTime.now());
        if (expiredRecords.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (InventoryLockRecord record : expiredRecords) {
            String lockKey = INVENTORY_LOCK_KEY + record.getProductId();
            boolean locked = distributedLock.tryLock(lockKey, 3, 20, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("超时释放库存获取锁失败，跳过: lockNo={}", record.getLockNo());
                continue;
            }

            try {
                int result = productInventoryMapper.rollbackStock(record.getProductId(), record.getLockQuantity());
                if (result > 0) {
                    baseMapper.releaseLock(record.getId(), LocalDateTime.now(),
                            InventoryReleaseTypeEnum.TIMEOUT_RELEASE.getCode(), null, "超时自动释放");
                    if (enableAntiOversell) {
                        clearStockCache(record.getProductId());
                    }
                    successCount++;
                    log.info("超时释放库存锁定成功: lockNo={}, productId={}, quantity={}",
                            record.getLockNo(), record.getProductId(), record.getLockQuantity());
                }
            } catch (Exception e) {
                log.error("超时释放库存锁定异常: lockNo={}", record.getLockNo(), e);
            } finally {
                distributedLock.unlock(lockKey);
            }
        }
        return successCount;
    }

    public List<InventoryLockRecord> findAbnormalLocks() {
        LocalDateTime abnormalTime = LocalDateTime.now().plusMinutes(lockExpireMinutes + 60);
        LambdaQueryWrapper<InventoryLockRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLockRecord::getLockStatus, InventoryLockStatusEnum.LOCKING.getCode())
               .lt(InventoryLockRecord::getLockExpireTime, LocalDateTime.now().minusMinutes(30))
               .orderByAsc(InventoryLockRecord::getLockExpireTime);
        return list(wrapper);
    }

    private void clearStockCache(Long productId) {
        String cacheKey = INVENTORY_STOCK_KEY + productId;
        redisTemplate.delete(cacheKey);
    }
}
