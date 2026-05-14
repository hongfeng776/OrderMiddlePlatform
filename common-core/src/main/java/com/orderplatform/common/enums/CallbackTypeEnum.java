package com.orderplatform.common.enums;

import lombok.Getter;

@Getter
public enum CallbackTypeEnum {
    
    PAYMENT_CALLBACK(1, "支付回调"),
    REFUND_CALLBACK(2, "退款回调");
    
    private final Integer code;
    private final String desc;
    
    CallbackTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static CallbackTypeEnum getByCode(Integer code) {
        for (CallbackTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
