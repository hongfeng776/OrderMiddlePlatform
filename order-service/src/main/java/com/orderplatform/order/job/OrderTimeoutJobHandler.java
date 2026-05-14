package com.orderplatform.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.common.enums.OrderStatusEnum;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.feign.InventoryFeignClient;
import com.orderplatform.order.service.OrderAsyncService;
import com.orderplatform.order.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutJobHandler extends AbstractJobHandler {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private OrderAsyncService orderAsyncService;

    @Value("${order.timeout-minutes:15}")
    private int timeoutMinutes;

    @Override
    protected String getJobName() {
        return "订单超时取消";
    }

    @Override
    protected String getJobHandler() {
        return "orderTimeoutCancelJobHandler";
    }

    @Override
    protected void doExecute(String param, int shardIndex, int shardTotal) throws Exception {
        int timeout = timeoutMinutes;
        if (param != null && !param.isEmpty()) {
            try {
                timeout = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("超时参数格式错误，使用默认值: {}", param);
            }
        }

        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeout);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .lt(Order::getCreateTime, timeoutTime);

        if (shardTotal > 1) {
            wrapper.apply("MOD(id, {0}) = {1}", shardTotal, shardIndex);
        }

        List<Order> timeoutOrders = orderService.list(wrapper);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;
        for (Order order : timeoutOrders) {
            if (cancelOrder(order)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        String result = String.format("分片[%d/%d] 处理完成: 总数=%d, 成功=%d, 失败=%d",
                shardIndex, shardTotal, timeoutOrders.size(), successCount, failCount);
        log.info(result);
    }

    private boolean cancelOrder(Order order) {
        try {
            log.info("开始取消超时订单: orderNo={}, createTime={}", order.getOrderNo(), order.getCreateTime());

            inventoryFeignClient.rollbackOrderStock(order.getOrderNo());
            log.info("订单库存回滚完成: orderNo={}", order.getOrderNo());

            order.setOrderStatus(OrderStatusEnum.CANCELLED.getCode());
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

    @XxlJob("orderTimeoutCancelJobHandler")
    public void executeJob() {
        super.execute();
    }
}
