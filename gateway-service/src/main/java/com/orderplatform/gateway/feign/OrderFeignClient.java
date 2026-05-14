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
    Result<Boolean> paySuccess(@PathVariable("orderNo") String orderNo);

    @GetMapping("/order/list/{userId}")
    Result<List> listByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/order/{orderNo}")
    Result getOrderDetail(@PathVariable("orderNo") String orderNo);
}
