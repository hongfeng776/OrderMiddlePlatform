package com.orderplatform.inventory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InventoryAsyncService {

    @Async("inventoryTaskExecutor")
    public void recordInventoryChangeLog(Long productId, String productName, Integer beforeStock, 
                                         Integer afterStock, String orderNo, String type) {
        log.info("【库存变更日志】商品ID:{}, 商品名称:{}, 变更前:{}, 变更后:{}, 订单号:{}, 变更类型:{}, 时间:{}",
                productId, productName, beforeStock, afterStock, orderNo, type, LocalDateTime.now());
    }

    @Async("notificationTaskExecutor")
    public void sendLowStockAlert(Long productId, String productName, Integer currentStock) {
        if (currentStock < 10) {
            log.warn("【库存预警】商品 {} (ID:{}) 库存不足，当前库存:{}", productName, productId, currentStock);
        }
    }

    @Async("notificationTaskExecutor")
    public void sendStockDeductionNotification(Long productId, String productName, Integer quantity, String orderNo) {
        log.info("【库存扣减通知】商品 {} (ID:{}) 扣减数量:{}, 订单号:{}", productName, productId, quantity, orderNo);
    }
}
