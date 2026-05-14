package com.orderplatform.gateway.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {

    @PostMapping("/payment/create")
    Result<String> createPayment(@RequestBody Map<String, Object> params);

    @PostMapping("/payment/process/{payNo}")
    Result<Boolean> processPayment(@PathVariable("payNo") String payNo);
}
