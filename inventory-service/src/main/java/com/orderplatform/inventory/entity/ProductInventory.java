package com.orderplatform.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_inventory")
public class ProductInventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer stockNum;

    private Integer lockStock;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
