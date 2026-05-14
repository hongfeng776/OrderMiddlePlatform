package com.orderplatform.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.feign.InventoryFeignClient;
import com.orderplatform.order.service.OrderAsyncService;
import com.orderplatform.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
public class OrderTimeoutTask {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private OrderAsyncService orderAsyncService;

    @Resource
    private RedisDistributedLock distributedLock;

    @Value("${order.timeout-minutes:15}")
    private int timeoutMinutes;

    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutOrders() {
        String lockKey = "order_timeout_task_lock";
        boolean locked = distributedLock.tryLock(lockKey, 5, TimeUnit.MINUTES);
        if (!locked) {
            log.debug("订单超时任务正在执行，跳过本次执行");
            return;
        }

        try {
            LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
            
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderStatus, 0)
                    .lt(Order::getCreateTime, timeoutTime);
            
            List<Order> timeoutOrders = orderService.list(wrapper);
            
            if (timeoutOrders.isEmpty()) {
                return;
            }
            
            log.info("发现{}个超时订单需要处理", timeoutOrders.size());
            
            int successCount = 0;
            for (Order order : timeoutOrders) {
                if (cancelOrder(order)) {
                    successCount++;
                }
            }
            
            log.info("超时订单处理完成: 总数={}, 成功={}", timeoutOrders.size(), successCount);
        } catch (Exception e) {
            log.error("处理超时订单异常", e);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private boolean cancelOrder(Order order) {
        try {
            log.info("开始取消超时订单: orderNo={}, createTime={}", order.getOrderNo(), order.getCreateTime());
            
            Map<String, Object> releaseParams = new HashMap<>();
            releaseParams.put("releaseType", 3);
            releaseParams.put("remark", "订单超时自动取消，释放库存");
            inventoryFeignClient.releaseLockByOrderNo(order.getOrderNo(), releaseParams);
            log.info("订单库存锁定释放完成: orderNo={}", order.getOrderNo());
            
            order.setOrderStatus(5);
            order.setCancelTime(LocalDateTime.now());
            orderService.updateById(order);
            
            orderAsyncService.sendOrderTimeoutNotification(order.getOrderNo(), order.getUserId());
            
            log.info("超时订单已取消: orderNo={}", order.getOrderNo());
            return true;
        } catch (Exception e) {
            log.error("取消超时订单失败: orderNo={}", order.getOrderNo(), e);
            return false;
        }
    }
}
