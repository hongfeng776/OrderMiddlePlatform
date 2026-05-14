package com.orderplatform.common.enums;

import lombok.Getter;

@Getter
public enum PayStatusEnum {
    
    PENDING(0, "待支付"),
    SUCCESS(1, "支付成功"),
    FAILED(2, "支付失败");
    
    private final Integer code;
    private final String desc;
    
    PayStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static PayStatusEnum getByCode(Integer code) {
        for (PayStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
