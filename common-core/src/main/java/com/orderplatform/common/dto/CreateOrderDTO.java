package com.orderplatform.common.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderDTO {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID无效")
    private Long userId;

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人姓名过长")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 500, message = "收货地址过长")
    private String receiverAddress;

    @Size(max = 1000, message = "备注信息过长")
    private String remark;

    @NotEmpty(message = "商品列表不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID无效")
        private Long productId;

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 200, message = "商品名称过长")
        private String productName;

        private String productImage;

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        private BigDecimal productPrice;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为1")
        @Max(value = 100, message = "购买数量不能超过100")
        private Integer buyCount;
    }
}
