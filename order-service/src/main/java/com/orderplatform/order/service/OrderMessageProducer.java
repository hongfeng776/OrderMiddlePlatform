package com.orderplatform.order.service;

import com.orderplatform.common.enums.OrderStatusEnum;
import com.orderplatform.common.util.MessageIdGenerator;
import com.orderplatform.order.config.RabbitMQConfig;
import com.orderplatform.order.dto.NotificationMessage;
import com.orderplatform.order.dto.OrderStatusMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void sendOrderStatusChange(String orderNo, Long userId, Integer previousStatus, Integer currentStatus, 
                                       String actionType, String remark) {
        String messageId = MessageIdGenerator.generateOrderStatusId(orderNo);
        try {
            OrderStatusMessage message = new OrderStatusMessage();
            message.setMessageId(messageId);
            message.setOrderNo(orderNo);
            message.setUserId(userId);
            message.setOperatorType("USER");
            message.setOperatorName("用户");
            message.setPreviousStatus(previousStatus);
            message.setCurrentStatus(currentStatus);
            OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(currentStatus);
            message.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知状态");
            message.setActionType(actionType);
            message.setRemark(remark);
            message.setChangeTime(LocalDateTime.now());
            message.setSendTime(LocalDateTime.now());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_STATUS_EXCHANGE,
                    RabbitMQConfig.ORDER_STATUS_ROUTING_KEY,
                    message
            );

            log.info("发送订单状态变更消息成功: messageId={}, orderNo={}, from={}, to={}", 
                    messageId, orderNo, previousStatus, currentStatus);
        } catch (Exception e) {
            log.error("发送订单状态变更消息失败: messageId={}, orderNo={}", messageId, orderNo, e);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, String orderNo, String title, String content, Integer type) {
        String messageId = MessageIdGenerator.generateNotificationId(userId);
        try {
            NotificationMessage message = new NotificationMessage();
            message.setMessageId(messageId);
            message.setUserId(userId);
            message.setOrderNo(orderNo);
            message.setTitle(title);
            message.setContent(content);
            message.setType(type);
            message.setCreateTime(LocalDateTime.now());
            message.setSendTime(LocalDateTime.now());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    message
            );

            log.info("发送通知消息成功: messageId={}, userId={}, orderNo={}, title={}", 
                    messageId, userId, orderNo, title);
        } catch (Exception e) {
            log.error("发送通知消息失败: messageId={}, userId={}, orderNo={}", messageId, userId, orderNo, e);
            throw e;
        }
    }
}
