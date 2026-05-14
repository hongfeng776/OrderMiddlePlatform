package com.orderplatform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String operatorType;

    private String operatorName;

    private Integer previousStatus;

    private Integer currentStatus;

    private String actionType;

    private String remark;

    private LocalDateTime createTime;
}
