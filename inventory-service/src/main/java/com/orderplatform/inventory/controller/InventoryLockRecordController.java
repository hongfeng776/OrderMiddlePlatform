package com.orderplatform.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderplatform.common.result.Result;
import com.orderplatform.inventory.entity.InventoryLockRecord;
import com.orderplatform.inventory.enums.InventoryLockTypeEnum;
import com.orderplatform.inventory.enums.InventoryReleaseTypeEnum;
import com.orderplatform.inventory.service.InventoryLockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/inventory-lock")
public class InventoryLockRecordController {

    @Autowired
    private InventoryLockRecordService inventoryLockRecordService;

    @GetMapping("/list")
    public Result<Page<InventoryLockRecord>> listLockRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer lockStatus) {
        Page<InventoryLockRecord> result = inventoryLockRecordService.listLockRecords(page, size, productId, orderNo, lockStatus);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<InventoryLockRecord> getById(@PathVariable Long id) {
        InventoryLockRecord record = inventoryLockRecordService.getById(id);
        return Result.success(record);
    }

    @PostMapping("/create")
    public Result<InventoryLockRecord> createLockRecord(@RequestBody Map<String, Object> params) {
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        String orderNo = (String) params.get("orderNo");
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer lockType = params.containsKey("lockType") ? Integer.valueOf(params.get("lockType").toString()) : InventoryLockTypeEnum.ORDER_PRE_DEDUCT.getCode();

        InventoryLockRecord record = inventoryLockRecordService.createLockRecord(productId, quantity, orderNo, userId, lockType);
        return Result.success(record);
    }

    @PostMapping("/confirm/{orderNo}")
    public Result<Boolean> confirmLockByOrderNo(@PathVariable String orderNo) {
        boolean result = inventoryLockRecordService.confirmLockByOrderNo(orderNo);
        return Result.success(result);
    }

    @PostMapping("/release-order/{orderNo}")
    public Result<Boolean> releaseLockByOrderNo(@PathVariable String orderNo, @RequestBody(required = false) Map<String, Object> params) {
        Integer releaseType = params != null && params.containsKey("releaseType") ? Integer.valueOf(params.get("releaseType").toString()) : InventoryReleaseTypeEnum.CANCEL_ORDER_RELEASE.getCode();
        Long operatorId = params != null && params.containsKey("operatorId") ? Long.valueOf(params.get("operatorId").toString()) : null;
        String remark = params != null ? (String) params.get("remark") : null;

        boolean result = inventoryLockRecordService.releaseLockByOrderNo(orderNo, releaseType, operatorId, remark);
        return Result.success(result);
    }

    @PostMapping("/manual-release/{id}")
    public Result<Boolean> manualReleaseLock(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Long operatorId = params.containsKey("operatorId") ? Long.valueOf(params.get("operatorId").toString()) : null;
        String remark = (String) params.get("remark");

        boolean result = inventoryLockRecordService.manualReleaseLock(id, operatorId, remark);
        return Result.success(result);
    }

    @PostMapping("/release-expired")
    public Result<Integer> releaseExpiredLocks() {
        int count = inventoryLockRecordService.releaseExpiredLocks();
        return Result.success(count);
    }

    @PostMapping("/force-release/{id}")
    public Result<Boolean> forceReleaseLock(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Long operatorId = params.containsKey("operatorId") ? Long.valueOf(params.get("operatorId").toString()) : null;
        String remark = (String) params.get("remark");

        boolean result = inventoryLockRecordService.forceReleaseLock(id, operatorId, remark);
        return Result.success(result);
    }

    @GetMapping("/abnormal")
    public Result<List<InventoryLockRecord>> getAbnormalLocks() {
        List<InventoryLockRecord> list = inventoryLockRecordService.findAbnormalLocks();
        return Result.success(list);
    }
}
