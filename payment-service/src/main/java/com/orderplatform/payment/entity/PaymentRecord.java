package com.orderplatform.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String orderNo;

    private Long userId;

    private BigDecimal payAmount;

    private Integer payType;

    private Integer payStatus;

    private LocalDateTime payTime;

    private String thirdPartyNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
