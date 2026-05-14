package com.orderplatform.inventory.job;

import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.inventory.service.InventoryLockRecordService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InventoryLockReleaseJobHandler {

    @Autowired
    private InventoryLockRecordService inventoryLockRecordService;

    @Autowired
    private RedisDistributedLock distributedLock;

    @XxlJob("inventoryLockReleaseJobHandler")
    public void inventoryLockReleaseJob() {
        String lockKey = "inventory_lock_release_task_lock";
        boolean locked = distributedLock.tryLock(lockKey, 5, TimeUnit.MINUTES);
        if (!locked) {
            log.debug("库存锁定超时释放任务正在执行，跳过本次执行");
            XxlJobHelper.log("库存锁定超时释放任务正在执行，跳过本次执行");
            return;
        }

        try {
            XxlJobHelper.log("开始执行库存锁定超时释放任务");
            log.info("开始执行库存锁定超时释放任务");

            int releasedCount = inventoryLockRecordService.releaseExpiredLocks();

            String result = String.format("库存锁定超时释放任务完成: 释放锁定记录数=%d", releasedCount);
            XxlJobHelper.log(result);
            log.info(result);

        } catch (Exception e) {
            log.error("执行库存锁定超时释放任务异常", e);
            XxlJobHelper.log("执行库存锁定超时释放任务异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        } finally {
            distributedLock.unlock(lockKey);
        }
    }
}
