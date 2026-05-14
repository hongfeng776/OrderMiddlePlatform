package com.orderplatform.gateway.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryFeignClient {

    @GetMapping("/inventory/list")
    Result<List> list();

    @GetMapping("/inventory/{productId}")
    Result getByProductId(@PathVariable("productId") Long productId);

    @PostMapping("/inventory/lock")
    Result<Boolean> lockStock(@RequestBody Map<String, Object> params);

    @PostMapping("/inventory/unlock")
    Result<Boolean> unlockStock(@RequestBody Map<String, Object> params);

    @PostMapping("/inventory/pre-deduct")
    Result<Boolean> preDeductStock(@RequestBody Map<String, Object> params);

    @PostMapping("/inventory/confirm-deduct")
    Result<Boolean> confirmDeductStock(@RequestBody Map<String, Object> params);

    @PostMapping("/inventory/rollback")
    Result<Boolean> rollbackStock(@RequestBody Map<String, Object> params);
}
