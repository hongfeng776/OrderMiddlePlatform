package com.orderplatform.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private String productName;

    private String productImage;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal productPrice;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer buyCount;
}
