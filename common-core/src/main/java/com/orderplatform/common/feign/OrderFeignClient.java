package com.orderplatform.common.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service", path = "/order")
public interface OrderFeignClient {

    @PostMapping("/pay-success/{orderNo}")
    Result<Boolean> paySuccess(@PathVariable("orderNo") String orderNo, 
                               @RequestBody(required = false) Map<String, String> body);

    @PostMapping("/refund-success/{orderNo}")
    Result<Boolean> refundSuccess(@PathVariable("orderNo") String orderNo,
                                  @RequestBody(required = false) Map<String, String> body);
}
