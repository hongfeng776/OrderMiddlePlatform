package com.orderplatform.order.controller;

import com.orderplatform.common.annotation.Idempotent;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.dto.CreateOrderDTO;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    @Idempotent(expireTime = 1, message = "订单正在处理中，请勿重复提交")
    public Result<Order> createOrder(@RequestBody @Valid CreateOrderDTO dto) {
        log.info("收到创建订单请求: {}", dto);
        Order order = orderService.createOrder(dto);
        return Result.success(order);
    }

    @GetMapping("/list/{userId}")
    public Result<List<Order>> listOrders(@PathVariable Long userId) {
        List<Order> orders = orderService.listByUserId(userId);
        return Result.success(orders);
    }

    @GetMapping("/{orderNo}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable String orderNo) {
        Order order = orderService.getOrderByNo(orderNo);
        List<com.orderplatform.order.entity.OrderItem> items = orderService.getOrderItems(order.getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        
        return Result.success(data);
    }

    @PostMapping("/pay-success/{orderNo}")
    public Result<Boolean> paySuccess(@PathVariable String orderNo) {
        boolean result = orderService.paySuccess(orderNo);
        return Result.success(result);
    }
}
