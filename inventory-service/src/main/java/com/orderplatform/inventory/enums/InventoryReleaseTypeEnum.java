package com.orderplatform.inventory.enums;

import lombok.Getter;

@Getter
public enum InventoryReleaseTypeEnum {
    TIMEOUT_RELEASE(1, "超时释放"),
    MANUAL_RELEASE(2, "手动释放"),
    CANCEL_ORDER_RELEASE(3, "取消订单释放");

    private final Integer code;
    private final String desc;

    InventoryReleaseTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
