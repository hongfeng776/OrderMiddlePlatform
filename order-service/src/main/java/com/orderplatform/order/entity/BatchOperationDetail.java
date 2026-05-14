package com.orderplatform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("batch_operation_detail")
public class BatchOperationDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String orderNo;

    private Integer success;

    private String errorMsg;

    private LocalDateTime createTime;
}
