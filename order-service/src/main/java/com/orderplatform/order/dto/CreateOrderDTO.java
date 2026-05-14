package com.orderplatform.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderDTO {

    private Long userId;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private String productName;
        private String productImage;
        private java.math.BigDecimal productPrice;
        private Integer buyCount;
    }
}
