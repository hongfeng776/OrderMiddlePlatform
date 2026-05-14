package com.orderplatform.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class OrderAsyncService {

    @Async("orderTaskExecutor")
    public void recordOrderLog(String orderNo, Long userId, BigDecimal totalAmount, String status) {
        log.info("【订单日志】订单号:{}, 用户ID:{}, 金额:{}, 状态:{}, 时间:{}",
                orderNo, userId, totalAmount, status, LocalDateTime.now());
    }

    @Async("notificationTaskExecutor")
    public void sendOrderCreatedNotification(String orderNo, Long userId) {
        log.info("【订单创建通知】订单号:{}, 用户ID:{}", orderNo, userId);
    }

    @Async("notificationTaskExecutor")
    public void sendPaymentSuccessNotification(String orderNo, Long userId) {
        log.info("【支付成功通知】订单号:{}, 用户ID:{}", orderNo, userId);
    }

    @Async("notificationTaskExecutor")
    public void sendOrderTimeoutNotification(String orderNo, Long userId) {
        log.info("【订单超时通知】订单号:{}, 用户ID:{} 订单已超时取消", orderNo, userId);
    }
}
