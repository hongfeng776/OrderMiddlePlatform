package com.orderplatform.common.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    PENDING_SHIPMENT(2, "待发货"),
    SHIPPED(3, "已发货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    REFUNDED(6, "已退款");
    
    private final Integer code;
    private final String desc;
    
    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
