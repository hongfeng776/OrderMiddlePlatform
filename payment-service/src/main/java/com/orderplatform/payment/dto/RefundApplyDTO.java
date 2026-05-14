package com.orderplatform.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RefundApplyDTO {

    private String orderNo;

    private String payNo;

    private Long userId;

    private BigDecimal refundAmount;

    private String refundReason;

    private List<String> refundVouchers;
}
