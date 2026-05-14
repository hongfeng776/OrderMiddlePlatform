package com.orderplatform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("batch_operation_log")
public class BatchOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String operationType;

    private Long operatorId;

    private String operatorName;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String detail;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
