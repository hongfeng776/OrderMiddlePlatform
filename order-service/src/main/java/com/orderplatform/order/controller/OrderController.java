package com.orderplatform.order.controller;

import com.orderplatform.common.annotation.Idempotent;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.dto.CreateOrderDTO;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderStatusLog;
import com.orderplatform.order.service.NotificationService;
import com.orderplatform.order.service.OrderService;
import com.orderplatform.order.service.OrderStatusLogService;
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

    @Autowired
    private OrderStatusLogService orderStatusLogService;

    @Autowired
    private NotificationService notificationService;

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
        List<OrderStatusLog> statusHistory = orderStatusLogService.getOrderStatusHistory(orderNo);
        
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        data.put("statusHistory", statusHistory);
        
        return Result.success(data);
    }

    @PostMapping("/pay-success/{orderNo}")
    public Result<Boolean> paySuccess(@PathVariable String orderNo, 
                                      @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        boolean result = orderService.paySuccess(orderNo, remark);
        return Result.success(result);
    }

    @PostMapping("/ship/{orderNo}")
    public Result<Boolean> ship(@PathVariable String orderNo,
                                 @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        boolean result = orderService.ship(orderNo, remark);
        return Result.success(result);
    }

    @PostMapping("/refund-success/{orderNo}")
    public Result<Boolean> refundSuccess(@PathVariable String orderNo,
                                         @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        boolean result = orderService.refundSuccess(orderNo, remark);
        return Result.success(result);
    }

    @PostMapping("/complete/{orderNo}")
    public Result<Boolean> complete(@PathVariable String orderNo,
                                     @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        boolean result = orderService.complete(orderNo, remark);
        return Result.success(result);
    }

    @PostMapping("/cancel/{orderNo}")
    public Result<Boolean> cancel(@PathVariable String orderNo,
                                   @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        boolean result = orderService.cancel(orderNo, remark);
        return Result.success(result);
    }

    @GetMapping("/status-history/{orderNo}")
    public Result<List<OrderStatusLog>> getStatusHistory(@PathVariable String orderNo) {
        List<OrderStatusLog> history = orderStatusLogService.getOrderStatusHistory(orderNo);
        return Result.success(history);
    }

    @GetMapping("/notifications/{userId}")
    public Result<List<Notification>> getNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return Result.success(notifications);
    }

    @GetMapping("/notifications/unread-count/{userId}")
    public Result<Long> getUnreadCount(@PathVariable Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PostMapping("/notifications/mark-read/{id}")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        boolean result = notificationService.markAsRead(id);
        return Result.success(result);
    }

    @PostMapping("/notifications/mark-batch-read/{userId}")
    public Result<Boolean> markBatchAsRead(@PathVariable Long userId,
                                            @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        boolean result = notificationService.markBatchAsRead(userId, ids);
        return Result.success(result);
    }

    @PostMapping("/notifications/mark-all-read/{userId}")
    public Result<Boolean> markAllAsRead(@PathVariable Long userId) {
        boolean result = notificationService.markAllAsRead(userId);
        return Result.success(result);
    }
}
