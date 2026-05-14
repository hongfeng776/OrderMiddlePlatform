package com.orderplatform.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    USER_NOT_EXIST(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXIST(1003, "用户已存在"),

    INVENTORY_NOT_ENOUGH(2001, "库存不足"),
    PRODUCT_NOT_EXIST(2002, "商品不存在"),

    ORDER_NOT_EXIST(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态错误"),

    PAYMENT_FAIL(4001, "支付失败");

    private final Integer code;
    private final String message;
}
