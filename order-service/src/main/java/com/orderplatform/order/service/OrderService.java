package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.result.Result;
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
    private OrderAsyncService orderAsyncService;

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
                
                Result<Boolean> preDeductResult = inventoryFeignClient.preDeductStock(preDeductParams);
                if (!preDeductResult.getCode().equals(200) || !Boolean.TRUE.equals(preDeductResult.getData())) {
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
            order.setOrderStatus(0);
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
            
            orderAsyncService.recordOrderLog(orderNo, dto.getUserId(), totalAmount, "CREATED");
            orderAsyncService.sendOrderCreatedNotification(orderNo, dto.getUserId());
            
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

    @Transactional(rollbackFor = Exception.class)
    public boolean paySuccess(String orderNo) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getOrderStatus() != 0) {
            throw new BusinessException("订单状态异常");
        }
        
        List<OrderItem> orderItems = getOrderItems(order.getId());
        
        for (OrderItem item : orderItems) {
            Map<String, Object> confirmParams = new HashMap<>();
            confirmParams.put("productId", item.getProductId());
            confirmParams.put("count", item.getBuyCount());
            confirmParams.put("orderNo", orderNo);
            
            Result<Boolean> confirmResult = inventoryFeignClient.confirmDeductStock(confirmParams);
            if (!confirmResult.getCode().equals(200) || !Boolean.TRUE.equals(confirmResult.getData())) {
                log.warn("确认扣减库存失败，继续处理: orderNo={}, productId={}", orderNo, item.getProductId());
            }
        }
        
        order.setOrderStatus(1);
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        
        orderAsyncService.sendPaymentSuccessNotification(orderNo, order.getUserId());
        
        log.info("支付成功: orderNo={}", orderNo);
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
