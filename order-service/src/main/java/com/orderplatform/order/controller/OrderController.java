package com.orderplatform.order.controller;

import com.orderplatform.common.annotation.Idempotent;
import com.orderplatform.common.annotation.RequiresPermission;
import com.orderplatform.common.enums.PermissionConstants;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.dto.BatchOperationDTO;
import com.orderplatform.order.dto.CreateOrderDTO;
import com.orderplatform.order.dto.OrderExportDTO;
import com.orderplatform.order.entity.BatchOperationDetail;
import com.orderplatform.order.entity.BatchOperationLog;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderStatusLog;
import com.orderplatform.order.service.BatchOperationService;
import com.orderplatform.order.service.MinIOService;
import com.orderplatform.order.service.NotificationService;
import com.orderplatform.order.service.OrderExportService;
import com.orderplatform.order.service.OrderService;
import com.orderplatform.order.service.OrderStatusLogService;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private BatchOperationService batchOperationService;

    @Autowired
    private OrderExportService orderExportService;

    @Autowired
    private MinIOService minIOService;

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

    @GetMapping("/admin/list")
    @RequiresPermission(PermissionConstants.ORDER_ADMIN)
    public Result<List<Order>> adminListOrders(
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNo) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (orderStatus != null) {
            wrapper.eq(Order::getOrderStatus, orderStatus);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo.trim());
        }
        if (startTime != null) {
            wrapper.ge(Order::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        
        List<Order> orders = orderService.list(wrapper);
        return Result.success(orders);
    }

    @PostMapping("/admin/batch-ship")
    @RequiresPermission(PermissionConstants.BATCH_SHIP)
    public Result<Map<String, Object>> batchShip(@RequestBody BatchOperationDTO dto) {
        Map<String, Object> result = batchOperationService.batchShip(dto);
        return Result.success(result);
    }

    @PostMapping("/admin/batch-cancel")
    @RequiresPermission(PermissionConstants.BATCH_CANCEL)
    public Result<Map<String, Object>> batchCancel(@RequestBody BatchOperationDTO dto) {
        Map<String, Object> result = batchOperationService.batchCancel(dto);
        return Result.success(result);
    }

    @PostMapping("/admin/export")
    @RequiresPermission(PermissionConstants.ORDER_EXPORT)
    public Result<Map<String, Object>> exportOrders(@RequestBody OrderExportDTO dto) throws IOException {
        Map<String, Object> result = orderExportService.exportOrdersToMinIO(dto);
        return Result.success(result);
    }

    @GetMapping("/admin/export/download")
    @RequiresPermission(PermissionConstants.ORDER_EXPORT)
    public ResponseEntity<byte[]> downloadExport(
            @RequestParam String objectName) throws IOException {
        log.info("下载文件请求: {}", objectName);
        byte[] data = minIOService.downloadFileAsBytes(objectName);
        
        String fileName = objectName.substring(objectName.lastIndexOf('/') + 1);
        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(data.length);
        headers.set("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        
        log.info("文件下载完成: {}，大小: {} bytes", fileName, data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/admin/batch-logs")
    @RequiresPermission(PermissionConstants.ORDER_ADMIN)
    public Result<List<BatchOperationLog>> getBatchLogs(@RequestParam(required = false) String operationType) {
        List<BatchOperationLog> logs = batchOperationService.getBatchLogs(operationType);
        return Result.success(logs);
    }

    @GetMapping("/admin/batch-details/{batchNo}")
    @RequiresPermission(PermissionConstants.ORDER_ADMIN)
    public Result<List<BatchOperationDetail>> getBatchDetails(@PathVariable String batchNo) {
        List<BatchOperationDetail> details = batchOperationService.getBatchDetails(batchNo);
        return Result.success(details);
    }
}
