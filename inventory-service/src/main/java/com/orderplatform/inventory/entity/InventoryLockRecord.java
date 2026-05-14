package com.orderplatform.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inventory_lock_record")
public class InventoryLockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String lockNo;

    private Long productId;

    private String productName;

    private String orderNo;

    private Long userId;

    private Integer lockQuantity;

    private Integer lockStatus;

    private Integer lockType;

    private LocalDateTime lockExpireTime;

    private LocalDateTime confirmTime;

    private LocalDateTime releaseTime;

    private Integer releaseType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
