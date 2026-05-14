package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.dto.CreateOrderDTO;
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

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderDTO dto) {
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
            BigDecimal itemTotal = itemDTO.getProductPrice().multiply(new BigDecimal(itemDTO.getBuyCount()));
            totalAmount = totalAmount.add(itemTotal);
            
            Map<String, Object> lockParams = new HashMap<>();
            lockParams.put("productId", itemDTO.getProductId());
            lockParams.put("count", itemDTO.getBuyCount());
            
            Result<Boolean> lockResult = inventoryFeignClient.lockStock(lockParams);
            if (!lockResult.getCode().equals(200) || !Boolean.TRUE.equals(lockResult.getData())) {
                throw new BusinessException("商品 " + itemDTO.getProductName() + " 库存不足");
            }
            
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
        
        return order;
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
