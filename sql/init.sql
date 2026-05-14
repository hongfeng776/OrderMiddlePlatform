CREATE DATABASE IF NOT EXISTS order_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE order_platform;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `product_inventory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(255) COMMENT '商品图片',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `stock_num` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `lock_stock` INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';

CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `freight_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
    `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
    `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-待发货，3-已发货，4-已完成，5-已取消',
    `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态：0-未支付，1-已支付',
    `receiver_name` VARCHAR(50) COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) COMMENT '收货人电话',
    `receiver_address` VARCHAR(500) COMMENT '收货地址',
    `remark` VARCHAR(1000) COMMENT '订单备注',
    `pay_time` DATETIME COMMENT '支付时间',
    `ship_time` DATETIME COMMENT '发货时间',
    `finish_time` DATETIME COMMENT '完成时间',
    `cancel_time` DATETIME COMMENT '取消时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(255) COMMENT '商品图片',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    `buy_count` INT NOT NULL COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总金额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `pay_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_type` TINYINT NOT NULL COMMENT '支付方式：1-支付宝，2-微信，3-银行卡',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付成功，2-支付失败',
    `pay_time` DATETIME COMMENT '支付完成时间',
    `third_party_no` VARCHAR(128) COMMENT '第三方支付流水号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

CREATE TABLE IF NOT EXISTS `order_status_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '状态日志ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `previous_status` TINYINT COMMENT '之前状态',
    `current_status` TINYINT NOT NULL COMMENT '当前状态',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态日志表';

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_no` VARCHAR(64) COMMENT '订单编号',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-订单通知',
    `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0-未读，1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`) VALUES
('admin', '123456', '管理员', '13800138000'),
('test', '123456', '测试用户', '13800138001');

INSERT INTO `product_inventory` (`product_id`, `product_name`, `product_image`, `price`, `stock_num`) VALUES
(1, 'iPhone 15 Pro', 'https://example.com/iphone15.jpg', 7999.00, 100),
(2, 'MacBook Pro 14', 'https://example.com/macbook.jpg', 14999.00, 50),
(3, 'AirPods Pro', 'https://example.com/airpods.jpg', 1899.00, 200),
(4, 'iPad Air', 'https://example.com/ipad.jpg', 4799.00, 80),
(5, 'Apple Watch', 'https://example.com/watch.jpg', 2999.00, 150);
