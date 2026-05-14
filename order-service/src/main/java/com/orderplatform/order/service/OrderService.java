package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.enums.OrderStatusEnum;
import com.orderplatform.common.enums.OrderStatusTransition;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.dto.CreateOrderDTO;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderItem;
import com.orderplatform.order.feign.InventoryFeignClient;
import com.orderplatform.order.mapper.OrderItemMapper;
import com.orderplatform.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Autowired
    private SmsService smsService;

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderDTO dto) {
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<Map<String, Object>> lockedStocks = new ArrayList<>();
        
        try {
            for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
                BigDecimal itemTotal = itemDTO.getProductPrice().multiply(new BigDecimal(itemDTO.getBuyCount()));
                totalAmount = totalAmount.add(itemTotal);
                
                Map<String, Object> preDeductParams = new HashMap<>();
                preDeductParams.put("productId", itemDTO.getProductId());
                preDeductParams.put("count", itemDTO.getBuyCount());
                preDeductParams.put("orderNo", orderNo);
                
                Boolean preDeductResult = inventoryFeignClient.preDeductStock(preDeductParams).getData();
                if (!Boolean.TRUE.equals(preDeductResult)) {
                    throw new BusinessException("商品 " + itemDTO.getProductName() + " 库存不足");
                }
                lockedStocks.add(preDeductParams);
                
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo(orderNo);
                orderItem.setProductId(itemDTO.getProductId());
                orderItem.setProductName(itemDTO.getProductName());
                orderItem.setProductImage(itemDTO.getProductImage());
                orderItem.setProductPrice(itemDTO.getProductPrice());
                orderItem.setBuyCount(itemDTO.getBuyCount());
                orderItem.setTotalAmount(itemTotal);
                orderItem.setCreateTime(LocalDateTime.now());
                orderItems.add(orderItem);
            }
            
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(dto.getUserId());
            order.setTotalAmount(totalAmount);
            order.setPayAmount(totalAmount);
            order.setFreightAmount(BigDecimal.ZERO);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
            order.setPayStatus(0);
            order.setReceiverName(dto.getReceiverName());
            order.setReceiverPhone(dto.getReceiverPhone());
            order.setReceiverAddress(dto.getReceiverAddress());
            order.setRemark(dto.getRemark());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            save(order);
            
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
                orderItemMapper.insert(orderItem);
            }
            
            orderMessageProducer.sendOrderStatusChange(orderNo, dto.getUserId(), null, 
                OrderStatusEnum.PENDING_PAYMENT.getCode(), "创建订单", "用户创建订单");
            orderMessageProducer.sendNotification(dto.getUserId(), orderNo, "订单创建成功", "您的订单已创建成功，请及时支付", 1);
            smsService.sendOrderCreated(dto.getReceiverPhone(), orderNo);
            
            log.info("订单创建成功: orderNo={}", orderNo);
            return order;
            
        } catch (Exception e) {
            for (Map<String, Object> lockedStock : lockedStocks) {
                try {
                    inventoryFeignClient.rollbackStock(lockedStock);
                } catch (Exception ex) {
                    log.error("回滚库存失败: {}", lockedStock, ex);
                }
            }
            throw e;
        }
    }

    private void validateStatusTransition(String orderNo, Integer currentStatus, Integer nextStatus) {
        if (!OrderStatusTransition.canTransition(currentStatus, nextStatus)) {
            String currentStatusName = OrderStatusTransition.getStatusName(currentStatus);
            String nextStatusName = OrderStatusTransition.getStatusName(nextStatus);
            throw new BusinessException(
                String.format("订单状态不允许流转: 从[%s]到[%s]", currentStatusName, nextStatusName)
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundSuccess(String orderNo, String remark) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer previousStatus = order.getOrderStatus();
        
        order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderMessageProducer.sendOrderStatusChange(orderNo, order.getUserId(), previousStatus,
            OrderStatusEnum.REFUNDED.getCode(), "退款成功", remark != null ? remark : "订单退款完成");
        orderMessageProducer.sendNotification(order.getUserId(), orderNo, "退款成功",
            remark != null ? remark : "您的订单已退款完成", 1);
        
        log.info("退款成功: orderNo={}", orderNo);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean paySuccess(String orderNo) {
        return paySuccess(orderNo, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean paySuccess(String orderNo, String remark) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer previousStatus = order.getOrderStatus();
        validateStatusTransition(orderNo, previousStatus, OrderStatusEnum.PAID.getCode());
        
        List<OrderItem> orderItems = getOrderItems(order.getId());
        
        for (OrderItem item : orderItems) {
            Map<String, Object> confirmParams = new HashMap<>();
            confirmParams.put("productId", item.getProductId());
            confirmParams.put("count", item.getBuyCount());
            confirmParams.put("orderNo", orderNo);
            
            Boolean confirmResult = inventoryFeignClient.confirmDeductStock(confirmParams).getData();
            if (!Boolean.TRUE.equals(confirmResult)) {
                log.warn("确认扣减库存失败，继续处理: orderNo={}, productId={}", orderNo, item.getProductId());
            }
        }
        
        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderMessageProducer.sendOrderStatusChange(orderNo, order.getUserId(), previousStatus, 
            OrderStatusEnum.PAID.getCode(), "支付成功", remark != null ? remark : "用户完成支付");
        orderMessageProducer.sendNotification(order.getUserId(), orderNo, "支付成功", 
            remark != null ? remark : "您的订单已支付成功，等待商家发货", 1);
        smsService.sendPaymentSuccess(order.getReceiverPhone(), orderNo);
        
        log.info("支付成功: orderNo={}", orderNo);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String orderNo) {
        return ship(orderNo, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String orderNo, String remark) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer previousStatus = order.getOrderStatus();
        validateStatusTransition(orderNo, previousStatus, OrderStatusEnum.SHIPPED.getCode());
        
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderMessageProducer.sendOrderStatusChange(orderNo, order.getUserId(), previousStatus, 
            OrderStatusEnum.SHIPPED.getCode(), "订单已发货", remark != null ? remark : "商家已发货");
        orderMessageProducer.sendNotification(order.getUserId(), orderNo, "订单已发货", 
            remark != null ? remark : "您的订单已发货，请注意查收", 1);
        smsService.sendOrderShipped(order.getReceiverPhone(), orderNo);
        
        log.info("订单发货成功: orderNo={}", orderNo);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String orderNo) {
        return complete(orderNo, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String orderNo, String remark) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer previousStatus = order.getOrderStatus();
        validateStatusTransition(orderNo, previousStatus, OrderStatusEnum.COMPLETED.getCode());
        
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderMessageProducer.sendOrderStatusChange(orderNo, order.getUserId(), previousStatus, 
            OrderStatusEnum.COMPLETED.getCode(), "订单已完成", remark != null ? remark : "用户确认收货");
        orderMessageProducer.sendNotification(order.getUserId(), orderNo, "订单已完成", 
            remark != null ? remark : "您的订单已完成，感谢您的购买", 1);
        smsService.sendOrderCompleted(order.getReceiverPhone(), orderNo);
        
        log.info("订单完成: orderNo={}", orderNo);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String orderNo) {
        return cancel(orderNo, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String orderNo, String remark) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Integer previousStatus = order.getOrderStatus();
        validateStatusTransition(orderNo, previousStatus, OrderStatusEnum.CANCELLED.getCode());
        
        if (OrderStatusEnum.PAID.getCode().equals(previousStatus)) {
            List<OrderItem> orderItems = getOrderItems(order.getId());
            for (OrderItem item : orderItems) {
                Map<String, Object> rollbackParams = new HashMap<>();
                rollbackParams.put("productId", item.getProductId());
                rollbackParams.put("count", item.getBuyCount());
                rollbackParams.put("orderNo", orderNo);
                inventoryFeignClient.rollbackStock(rollbackParams);
            }
        }
        
        order.setOrderStatus(OrderStatusEnum.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderMessageProducer.sendOrderStatusChange(orderNo, order.getUserId(), previousStatus, 
            OrderStatusEnum.CANCELLED.getCode(), "订单已取消", remark != null ? remark : "用户取消订单");
        orderMessageProducer.sendNotification(order.getUserId(), orderNo, "订单已取消", 
            remark != null ? remark : "您的订单已取消", 1);
        smsService.sendOrderCancelled(order.getReceiverPhone(), orderNo);
        
        log.info("订单取消成功: orderNo={}", orderNo);
        return true;
    }

    public List<Order> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    public Order getOrderByNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
    }
}
