package com.orderplatform.inventory.enums;

import lombok.Getter;

@Getter
public enum InventoryLockStatusEnum {
    LOCKING(0, "锁定中"),
    CONFIRMED(1, "已确认扣减"),
    RELEASED(2, "已释放");

    private final Integer code;
    private final String desc;

    InventoryLockStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
