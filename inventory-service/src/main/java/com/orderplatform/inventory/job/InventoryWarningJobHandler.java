package com.orderplatform.inventory.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.inventory.entity.ProductInventory;
import com.orderplatform.inventory.service.InventoryAsyncService;
import com.orderplatform.inventory.service.ProductInventoryService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InventoryWarningJobHandler {

    @Autowired
    private ProductInventoryService productInventoryService;

    @Autowired
    private InventoryAsyncService inventoryAsyncService;

    @Autowired
    private RedisDistributedLock distributedLock;

    @Value("${inventory.warning-threshold:10}")
    private Integer warningThreshold;

    @XxlJob("inventoryWarningJobHandler")
    public void inventoryWarningJob() {
        String lockKey = "inventory_warning_task_lock";
        boolean locked = distributedLock.tryLock(lockKey, 5, TimeUnit.MINUTES);
        if (!locked) {
            log.debug("库存预警任务正在执行，跳过本次执行");
            XxlJobHelper.log("库存预警任务正在执行，跳过本次执行");
            return;
        }

        try {
            String param = XxlJobHelper.getJobParam();
            Integer threshold = warningThreshold;
            if (param != null && !param.isEmpty()) {
                try {
                    threshold = Integer.parseInt(param);
                } catch (NumberFormatException e) {
                    log.warn("预警阈值参数格式错误，使用默认值: {}", param);
                }
            }

            XxlJobHelper.log("开始执行库存预警任务, 预警阈值: {}", threshold);
            log.info("开始执行库存预警任务, 预警阈值: {}", threshold);

            LambdaQueryWrapper<ProductInventory> wrapper = new LambdaQueryWrapper<ProductInventory>()
                    .eq(ProductInventory::getStatus, 1)
                    .le(ProductInventory::getStockNum, threshold);

            List<ProductInventory> warningProducts = productInventoryService.list(wrapper);

            if (warningProducts.isEmpty()) {
                XxlJobHelper.log("未发现库存预警商品");
                return;
            }

            XxlJobHelper.log("发现{}个库存预警商品需要处理", warningProducts.size());
            log.info("发现{}个库存预警商品需要处理", warningProducts.size());

            int warningCount = 0;
            for (ProductInventory product : warningProducts) {
                try {
                    inventoryAsyncService.sendLowStockAlert(product.getProductId(),
                            product.getProductName(), product.getStockNum());
                    warningCount++;
                    XxlJobHelper.log("发送库存预警: productId={}, productName={}, stockNum={}",
                            product.getProductId(), product.getProductName(), product.getStockNum());
                } catch (Exception e) {
                    log.error("发送库存预警失败: productId={}", product.getProductId(), e);
                    XxlJobHelper.log("发送库存预警失败: productId={}, error={}",
                            product.getProductId(), e.getMessage());
                }
            }

            String result = String.format("库存预警任务完成: 预警商品数=%d, 发送成功=%d",
                    warningProducts.size(), warningCount);
            XxlJobHelper.log(result);
            log.info(result);

        } catch (Exception e) {
            log.error("执行库存预警任务异常", e);
            XxlJobHelper.log("执行库存预警任务异常: " + e.getMessage());
            XxlJobHelper.handleFail();
        } finally {
            distributedLock.unlock(lockKey);
        }
    }
}
