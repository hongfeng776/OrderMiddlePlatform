package com.orderplatform.gateway.feign;

import com.orderplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @PostMapping("/user/login")
    Result<Map<String, Object>> login(@RequestBody Map<String, String> params);

    @GetMapping("/user/{id}")
    Result getUserById(@PathVariable("id") Long id);
}
