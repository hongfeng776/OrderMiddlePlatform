package com.orderplatform.gateway.controller;

import com.orderplatform.common.result.Result;
import com.orderplatform.gateway.feign.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GatewayController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @PostMapping("/user/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return userFeignClient.login(params);
    }

    @GetMapping("/user/{id}")
    public Result getUserById(@PathVariable Long id) {
        return userFeignClient.getUserById(id);
    }

    @GetMapping("/inventory/list")
    public Result<List> listInventory() {
        return inventoryFeignClient.list();
    }

    @GetMapping("/inventory/{productId}")
    public Result getInventory(@PathVariable Long productId) {
        return inventoryFeignClient.getByProductId(productId);
    }

    @PostMapping("/inventory/lock")
    public Result<Boolean> lockStock(@RequestBody Map<String, Object> params) {
        return inventoryFeignClient.lockStock(params);
    }

    @PostMapping("/inventory/unlock")
    public Result<Boolean> unlockStock(@RequestBody Map<String, Object> params) {
        return inventoryFeignClient.unlockStock(params);
    }

    @PostMapping("/inventory/pre-deduct")
    public Result<Boolean> preDeductStock(@RequestBody Map<String, Object> params) {
        return inventoryFeignClient.preDeductStock(params);
    }

    @PostMapping("/inventory/confirm-deduct")
    public Result<Boolean> confirmDeductStock(@RequestBody Map<String, Object> params) {
        return inventoryFeignClient.confirmDeductStock(params);
    }

    @PostMapping("/inventory/rollback")
    public Result<Boolean> rollbackStock(@RequestBody Map<String, Object> params) {
        return inventoryFeignClient.rollbackStock(params);
    }

    @PostMapping("/order/create")
    public Result createOrder(@RequestBody Map<String, Object> params) {
        return orderFeignClient.createOrder(params);
    }

    @PostMapping("/order/pay-success/{orderNo}")
    public Result<Boolean> paySuccess(@PathVariable String orderNo) {
        return orderFeignClient.paySuccess(orderNo);
    }

    @GetMapping("/order/list/{userId}")
    public Result<List> listOrders(@PathVariable Long userId) {
        return orderFeignClient.listByUserId(userId);
    }

    @GetMapping("/order/{orderNo}")
    public Result getOrderDetail(@PathVariable String orderNo) {
        return orderFeignClient.getOrderDetail(orderNo);
    }

    @PostMapping("/payment/create")
    public Result<String> createPayment(@RequestBody Map<String, Object> params) {
        return paymentFeignClient.createPayment(params);
    }

    @PostMapping("/payment/process/{payNo}")
    public Result<Boolean> processPayment(@PathVariable String payNo) {
        return paymentFeignClient.processPayment(payNo);
    }
}
