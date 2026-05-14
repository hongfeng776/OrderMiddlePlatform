CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `config_desc` VARCHAR(200) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO `system_config` (`config_key`, `config_value`, `config_desc`) VALUES
('refund.timeout.hours', '72', '退款申请超时时间（小时），超时后自动驳回'),
('refund.timeout.enabled', 'true', '是否启用退款超时自动驳回功能');
