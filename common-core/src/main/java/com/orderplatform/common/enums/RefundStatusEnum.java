package com.orderplatform.common.enums;

import lombok.Getter;

@Getter
public enum RefundStatusEnum {
    
    PENDING_AUDIT(0, "待审核"),
    AUDIT_PASS(1, "审核通过"),
    AUDIT_REJECT(2, "审核拒绝"),
    REFUNDING(3, "退款中"),
    REFUND_SUCCESS(4, "退款成功"),
    REFUND_FAILED(5, "退款失败");
    
    private final Integer code;
    private final String desc;
    
    RefundStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static RefundStatusEnum getByCode(Integer code) {
        for (RefundStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
