USE order_platform;

CREATE TABLE IF NOT EXISTS `batch_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批量操作日志ID',
    `batch_no` VARCHAR(64) NOT NULL COMMENT '批量操作编号',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型：SHIP-批量发货，CANCEL-批量取消',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) COMMENT '操作人名称',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '总数量',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数量',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数量',
    `detail` TEXT COMMENT '操作详情JSON',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-处理中，1-处理完成，2-处理失败',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量操作日志表';

CREATE TABLE IF NOT EXISTS `batch_operation_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批量操作详情ID',
    `batch_no` VARCHAR(64) NOT NULL COMMENT '批量操作编号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `success` TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功：0-失败，1-成功',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量操作详情表';
