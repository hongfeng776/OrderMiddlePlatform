package com.orderplatform.order.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryFeignClient {

    @PostMapping("/inventory/lock")
    Result<Boolean> lockStock(@RequestBody Map<String, Object> params);

    @PostMapping("/inventory/unlock")
    Result<Boolean> unlockStock(@RequestBody Map<String, Object> params);
}
