package com.orderplatform.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum OrderStatusTransition {
    
    PENDING_PAYMENT(OrderStatusEnum.PENDING_PAYMENT, new OrderStatusEnum[]{
        OrderStatusEnum.PAID, 
        OrderStatusEnum.CANCELLED
    }),
    
    PAID(OrderStatusEnum.PAID, new OrderStatusEnum[]{
        OrderStatusEnum.SHIPPED, 
        OrderStatusEnum.CANCELLED
    }),
    
    SHIPPED(OrderStatusEnum.SHIPPED, new OrderStatusEnum[]{
        OrderStatusEnum.COMPLETED
    }),
    
    COMPLETED(OrderStatusEnum.COMPLETED, new OrderStatusEnum[]{}),
    
    CANCELLED(OrderStatusEnum.CANCELLED, new OrderStatusEnum[]{});
    
    private final OrderStatusEnum currentStatus;
    private final OrderStatusEnum[] allowedNextStatus;
    
    OrderStatusTransition(OrderStatusEnum currentStatus, OrderStatusEnum[] allowedNextStatus) {
        this.currentStatus = currentStatus;
        this.allowedNextStatus = allowedNextStatus;
    }
    
    public OrderStatusEnum getCurrentStatus() {
        return currentStatus;
    }
    
    public OrderStatusEnum[] getAllowedNextStatus() {
        return allowedNextStatus;
    }
    
    private static final Map<Integer, OrderStatusTransition> transitionMap = new HashMap<>();
    
    static {
        for (OrderStatusTransition transition : values()) {
            transitionMap.put(transition.getCurrentStatus().getCode(), transition);
        }
    }
    
    public static boolean canTransition(Integer currentStatusCode, Integer nextStatusCode) {
        if (currentStatusCode == null || nextStatusCode == null) {
            return false;
        }
        OrderStatusTransition transition = transitionMap.get(currentStatusCode);
        if (transition == null) {
            return false;
        }
        for (OrderStatusEnum nextStatus : transition.getAllowedNextStatus()) {
            if (nextStatus.getCode().equals(nextStatusCode)) {
                return true;
            }
        }
        return false;
    }
    
    public static String getStatusName(Integer statusCode) {
        OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(statusCode);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }
}
