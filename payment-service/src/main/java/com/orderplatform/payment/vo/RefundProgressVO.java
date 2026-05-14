package com.orderplatform.payment.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RefundProgressVO {

    private String refundNo;

    private String orderNo;

    private String payNo;

    private BigDecimal refundAmount;

    private String refundReason;

    private List<String> refundVouchers;

    private Integer refundStatus;

    private String refundStatusDesc;

    private List<ProgressStep> progressSteps;

    private String auditRemark;

    private LocalDateTime auditTime;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    @Data
    public static class ProgressStep {
        private String stepName;
        private String status;
        private LocalDateTime time;
        private String remark;
    }
}
