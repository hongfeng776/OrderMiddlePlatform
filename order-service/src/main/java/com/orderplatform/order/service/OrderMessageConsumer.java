package com.orderplatform.order.service;

import com.orderplatform.order.config.RabbitMQConfig;
import com.orderplatform.order.dto.NotificationMessage;
import com.orderplatform.order.dto.OrderStatusMessage;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.entity.OrderStatusLog;
import com.orderplatform.order.mapper.NotificationMapper;
import com.orderplatform.order.mapper.OrderStatusLogMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
public class OrderMessageConsumer {

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private MessageIdempotentService messageIdempotentService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderStatusChange(OrderStatusMessage message, Channel channel, Message amqpMessage) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId();
        
        try {
            log.info("接收订单状态变更消息: messageId={}, orderNo={}", messageId, message.getOrderNo());
            
            if (messageIdempotentService.isProcessed(messageId)) {
                log.warn("消息已处理，跳过: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            OrderStatusLog log = new OrderStatusLog();
            log.setOrderNo(message.getOrderNo());
            log.setUserId(message.getUserId());
            log.setOperatorType(message.getOperatorType());
            log.setOperatorName(message.getOperatorName());
            log.setPreviousStatus(message.getPreviousStatus());
            log.setCurrentStatus(message.getCurrentStatus());
            log.setActionType(message.getActionType());
            log.setRemark(message.getRemark() != null ? message.getRemark() : message.getStatusDesc());
            log.setCreateTime(message.getChangeTime() != null ? message.getChangeTime() : message.getSendTime());
            
            orderStatusLogMapper.insert(log);
            
            messageIdempotentService.markAsProcessed(messageId);
            
            channel.basicAck(deliveryTag, false);
            log.info("订单状态日志记录成功: messageId={}, orderNo={}", messageId, message.getOrderNo());
        } catch (DuplicateKeyException e) {
            log.warn("订单状态日志重复插入，已跳过: messageId={}, orderNo={}", messageId, message.getOrderNo());
            channel.basicAck(deliveryTag, false);
            messageIdempotentService.markAsProcessed(messageId);
        } catch (Exception e) {
            log.error("处理订单状态变更消息失败: messageId={}, orderNo={}", messageId, message.getOrderNo(), e);
            try {
                if (deliveryTag <= 3) {
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void handleNotification(NotificationMessage message, Channel channel, Message amqpMessage) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId();
        
        try {
            log.info("接收通知消息: messageId={}, userId={}, title={}", messageId, message.getUserId(), message.getTitle());
            
            if (messageIdempotentService.isProcessed(messageId)) {
                log.warn("消息已处理，跳过: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            Notification notification = new Notification();
            notification.setUserId(message.getUserId());
            notification.setOrderNo(message.getOrderNo());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setType(message.getType());
            notification.setReadStatus(0);
            notification.setCreateTime(message.getCreateTime() != null ? message.getCreateTime() : message.getSendTime());
            
            notificationMapper.insert(notification);
            
            messageIdempotentService.markAsProcessed(messageId);
            
            channel.basicAck(deliveryTag, false);
            log.info("通知存储成功: messageId={}, userId={}, title={}", messageId, message.getUserId(), message.getTitle());
        } catch (DuplicateKeyException e) {
            log.warn("通知重复插入，已跳过: messageId={}, userId={}, title={}", messageId, message.getUserId(), message.getTitle());
            channel.basicAck(deliveryTag, false);
            messageIdempotentService.markAsProcessed(messageId);
        } catch (Exception e) {
            log.error("处理通知消息失败: messageId={}, userId={}, title={}", messageId, message.getUserId(), message.getTitle(), e);
            try {
                if (deliveryTag <= 3) {
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }
}
