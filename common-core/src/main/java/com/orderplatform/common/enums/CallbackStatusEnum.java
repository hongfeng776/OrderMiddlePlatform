package com.orderplatform.common.enums;

import lombok.Getter;

@Getter
public enum CallbackStatusEnum {
    
    PENDING(0, "待处理"),
    SUCCESS(1, "处理成功"),
    FAILED(2, "处理失败");
    
    private final Integer code;
    private final String desc;
    
    CallbackStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static CallbackStatusEnum getByCode(Integer code) {
        for (CallbackStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
