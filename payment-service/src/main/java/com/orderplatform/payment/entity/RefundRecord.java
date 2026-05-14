package com.orderplatform.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String orderNo;

    private String payNo;

    private Long userId;

    private BigDecimal refundAmount;

    private String refundReason;

    private String refundVoucher;

    private Integer refundStatus;

    private Long auditUserId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime refundTime;

    private String thirdPartyRefundNo;

    private String idempotentKey;

    private Integer callbackCount;

    private LocalDateTime lastCallbackTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public List<String> getVoucherList() {
        if (refundVoucher == null || refundVoucher.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(refundVoucher.split(","));
    }
}
