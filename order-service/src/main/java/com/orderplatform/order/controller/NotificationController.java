package com.orderplatform.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public Result<Page<Notification>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer type) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PostMapping("/read/{id}")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        boolean success = notificationService.markAsRead(id);
        return Result.success(success);
    }

    @PostMapping("/read/batch")
    public Result<Boolean> markBatchAsRead(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        List<Long> ids = (List<Long>) params.get("ids");
        boolean success = notificationService.markBatchAsRead(userId, ids);
        return Result.success(success);
    }

    @PostMapping("/read/all")
    public Result<Boolean> markAllAsRead(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        boolean success = notificationService.markAllAsRead(userId);
        return Result.success(success);
    }
}
