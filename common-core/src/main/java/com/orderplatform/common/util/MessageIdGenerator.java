package com.orderplatform.common.util;

import java.util.UUID;

public class MessageIdGenerator {

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateOrderStatusId(String orderNo) {
        return "ORDER_STATUS_" + orderNo + "_" + System.currentTimeMillis();
    }

    public static String generateNotificationId(Long userId) {
        return "NOTIFICATION_" + userId + "_" + System.currentTimeMillis();
    }
}
