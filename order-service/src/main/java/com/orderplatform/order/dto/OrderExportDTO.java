package com.orderplatform.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderExportDTO {

    private Integer orderStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long userId;
}
