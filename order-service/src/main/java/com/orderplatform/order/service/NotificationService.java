package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    public List<Notification> getNotificationsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime));
    }

    public Long getUnreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long id) {
        Notification notification = getById(id);
        if (notification == null) {
            return false;
        }
        notification.setReadStatus(1);
        return updateById(notification);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markBatchAsRead(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            Notification notification = getById(id);
            if (notification != null && notification.getUserId().equals(userId)) {
                notification.setReadStatus(1);
                updateById(notification);
            }
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long userId) {
        List<Notification> notifications = list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, 0));
        
        for (Notification notification : notifications) {
            notification.setReadStatus(1);
            updateById(notification);
        }
        return true;
    }
}
