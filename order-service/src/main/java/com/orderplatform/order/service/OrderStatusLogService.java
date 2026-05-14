package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.enums.OrderStatusEnum;
import com.orderplatform.order.entity.OrderStatusLog;
import com.orderplatform.order.mapper.OrderStatusLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OrderStatusLogService extends ServiceImpl<OrderStatusLogMapper, OrderStatusLog> {

    public List<OrderStatusLog> getOrderStatusHistory(String orderNo) {
        List<OrderStatusLog> logs = list(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderNo, orderNo)
                .orderByAsc(OrderStatusLog::getId)
                .orderByAsc(OrderStatusLog::getCreateTime));
        
        logs.forEach(log -> {
            if (log.getRemark() == null || log.getRemark().isEmpty()) {
                if (log.getPreviousStatus() != null) {
                    OrderStatusEnum prevStatus = OrderStatusEnum.getByCode(log.getPreviousStatus());
                    OrderStatusEnum currStatus = OrderStatusEnum.getByCode(log.getCurrentStatus());
                    log.setRemark(String.format("从 %s 变更为 %s", 
                        prevStatus != null ? prevStatus.getDesc() : "无",
                        currStatus != null ? currStatus.getDesc() : "未知"));
                } else {
                    OrderStatusEnum currStatus = OrderStatusEnum.getByCode(log.getCurrentStatus());
                    log.setRemark(String.format("订单创建，状态为 %s", 
                        currStatus != null ? currStatus.getDesc() : "未知"));
                }
            }
        });
        
        return logs;
    }
}
