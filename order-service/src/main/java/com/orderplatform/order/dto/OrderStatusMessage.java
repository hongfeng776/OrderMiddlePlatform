package com.orderplatform.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusMessage implements Serializable {

    private String messageId;

    private String orderNo;

    private Long userId;

    private String operatorType;

    private String operatorName;

    private Integer previousStatus;

    private Integer currentStatus;

    private String statusDesc;

    private String actionType;

    private String remark;

    private LocalDateTime changeTime;

    private LocalDateTime sendTime;
}
