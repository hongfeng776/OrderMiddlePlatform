USE order_platform;

CREATE TABLE IF NOT EXISTS `inventory_lock_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存锁定记录ID',
    `lock_no` VARCHAR(64) NOT NULL COMMENT '锁定单号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `order_no` VARCHAR(64) NOT NULL COMMENT '关联订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `lock_quantity` INT NOT NULL COMMENT '锁定数量',
    `lock_status` TINYINT NOT NULL DEFAULT 0 COMMENT '锁定状态：0-锁定中，1-已确认扣减，2-已释放',
    `lock_type` TINYINT NOT NULL DEFAULT 1 COMMENT '锁定类型：1-下单预扣，2-手动锁定',
    `lock_expire_time` DATETIME NOT NULL COMMENT '锁定过期时间',
    `confirm_time` DATETIME COMMENT '确认扣减时间',
    `release_time` DATETIME COMMENT '释放时间',
    `release_type` TINYINT COMMENT '释放类型：1-超时释放，2-手动释放，3-取消订单释放',
    `operator_id` BIGINT COMMENT '操作人ID（手动解锁时使用）',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lock_no` (`lock_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_lock_status` (`lock_status`),
    KEY `idx_lock_expire_time` (`lock_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存锁定记录表';
