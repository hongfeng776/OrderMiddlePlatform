package com.orderplatform.inventory.controller;

import com.orderplatform.common.result.Result;
import com.orderplatform.inventory.entity.ProductInventory;
import com.orderplatform.inventory.service.ProductInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/inventory")
public class ProductInventoryController {

    @Autowired
    private ProductInventoryService productInventoryService;

    @GetMapping("/list")
    public Result<List<ProductInventory>> listAll() {
        List<ProductInventory> list = productInventoryService.listAll();
        return Result.success(list);
    }

    @GetMapping("/{productId}")
    public Result<ProductInventory> getByProductId(@PathVariable Long productId) {
        ProductInventory inventory = productInventoryService.getByProductId(productId);
        return Result.success(inventory);
    }

    @PostMapping("/lock")
    public Result<Boolean> lockStock(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        
        boolean result = productInventoryService.lockStock(productId, count);
        return Result.success(result);
    }

    @PostMapping("/unlock")
    public Result<Boolean> unlockStock(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        
        boolean result = productInventoryService.unlockStock(productId, count);
        return Result.success(result);
    }

    @PostMapping("/pre-deduct")
    public Result<Boolean> preDeductStock(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        String orderNo = (String) params.get("orderNo");
        
        boolean result = productInventoryService.preDeductStock(productId, count, orderNo);
        return Result.success(result);
    }

    @PostMapping("/confirm-deduct")
    public Result<Boolean> confirmDeductStock(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        String orderNo = (String) params.get("orderNo");
        
        boolean result = productInventoryService.confirmDeductStock(productId, count, orderNo);
        return Result.success(result);
    }

    @PostMapping("/rollback")
    public Result<Boolean> rollbackStock(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer count = Integer.valueOf(params.get("count").toString());
        String orderNo = (String) params.get("orderNo");
        
        boolean result = productInventoryService.rollbackStock(productId, count, orderNo);
        return Result.success(result);
    }
}
