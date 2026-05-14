package com.orderplatform.inventory.enums;

import lombok.Getter;

@Getter
public enum InventoryLockTypeEnum {
    ORDER_PRE_DEDUCT(1, "下单预扣"),
    MANUAL_LOCK(2, "手动锁定");

    private final Integer code;
    private final String desc;

    InventoryLockTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
