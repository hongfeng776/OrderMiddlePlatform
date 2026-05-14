package com.orderplatform.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private String messageId;

    private Long userId;

    private String orderNo;

    private String title;

    private String content;

    private Integer type;

    private LocalDateTime createTime;

    private LocalDateTime sendTime;
}
