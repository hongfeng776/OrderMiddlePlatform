package com.orderplatform.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("callback_log")
public class CallbackLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String callbackNo;

    private Integer callbackType;

    private String businessNo;

    private String orderNo;

    private String requestData;

    private String responseData;

    private Integer callbackStatus;

    private String errorMsg;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
