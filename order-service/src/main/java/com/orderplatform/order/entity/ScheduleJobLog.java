package com.orderplatform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("schedule_job_log")
public class ScheduleJobLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobName;

    private String jobHandler;

    private String jobParam;

    private String jobId;

    private Integer status;

    private String executeResult;

    private LocalDateTime executeTime;

    private Long duration;

    private String errorMessage;

    private Integer shardIndex;

    private Integer shardTotal;

    private String executorAddress;

    private Integer retryCount;

    private Integer alertStatus;

    private String alertMessage;

    private LocalDateTime alertTime;

    private LocalDateTime createTime;
}
