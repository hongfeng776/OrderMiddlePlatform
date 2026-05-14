package com.orderplatform.gateway.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @PostMapping("/order/create")
    Result createOrder(@RequestBody Map<String, Object> params);

    @PostMapping("/order/pay-success/{orderNo}")
    Result<Boolean> paySuccess(@PathVariable("orderNo") String orderNo,
                               @RequestBody(required = false) Map<String, String> body);

    @PostMapping("/order/ship/{orderNo}")
    Result<Boolean> ship(@PathVariable("orderNo") String orderNo,
                          @RequestBody(required = false) Map<String, String> body);

    @PostMapping("/order/complete/{orderNo}")
    Result<Boolean> complete(@PathVariable("orderNo") String orderNo,
                              @RequestBody(required = false) Map<String, String> body);

    @PostMapping("/order/cancel/{orderNo}")
    Result<Boolean> cancel(@PathVariable("orderNo") String orderNo,
                            @RequestBody(required = false) Map<String, String> body);

    @GetMapping("/order/list/{userId}")
    Result<List> listByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/order/{orderNo}")
    Result getOrderDetail(@PathVariable("orderNo") String orderNo);

    @GetMapping("/order/status-history/{orderNo}")
    Result getStatusHistory(@PathVariable("orderNo") String orderNo);

    @GetMapping("/order/notifications/{userId}")
    Result getNotifications(@PathVariable("userId") Long userId);

    @GetMapping("/order/notifications/unread-count/{userId}")
    Result getUnreadCount(@PathVariable("userId") Long userId);

    @PostMapping("/order/notifications/mark-read/{id}")
    Result markAsRead(@PathVariable("id") Long id);

    @PostMapping("/order/notifications/mark-batch-read/{userId}")
    Result markBatchAsRead(@PathVariable("userId") Long userId,
                            @RequestBody Map<String, List<Long>> body);

    @PostMapping("/order/notifications/mark-all-read/{userId}")
    Result markAllAsRead(@PathVariable("userId") Long userId);
}
