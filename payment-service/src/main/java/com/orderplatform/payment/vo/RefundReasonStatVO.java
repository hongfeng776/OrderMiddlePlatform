package com.orderplatform.payment.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundReasonStatVO {

    private String refundReason;

    private Long count;

    private BigDecimal totalAmount;

    private String percentage;
}
