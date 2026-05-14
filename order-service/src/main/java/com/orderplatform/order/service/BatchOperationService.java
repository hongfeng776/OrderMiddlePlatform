package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.enums.OrderStatusEnum;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.order.dto.BatchOperationDTO;
import com.orderplatform.order.entity.BatchOperationDetail;
import com.orderplatform.order.entity.BatchOperationLog;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.mapper.BatchOperationDetailMapper;
import com.orderplatform.order.mapper.BatchOperationLogMapper;
import com.orderplatform.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatchOperationService extends ServiceImpl<BatchOperationLogMapper, BatchOperationLog> {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BatchOperationDetailMapper detailMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    public Map<String, Object> batchShip(BatchOperationDTO dto) {
        String batchNo = "BATCH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("开始批量发货，批次号: {}，订单数量: {}", batchNo, dto.getOrderNos().size());

        BatchOperationLog batchLog = new BatchOperationLog();
        batchLog.setBatchNo(batchNo);
        batchLog.setOperationType("SHIP");
        batchLog.setOperatorId(dto.getOperatorId());
        batchLog.setOperatorName(dto.getOperatorName());
        batchLog.setTotalCount(dto.getOrderNos().size());
        batchLog.setSuccessCount(0);
        batchLog.setFailCount(0);
        batchLog.setStatus(0);
        batchLog.setCreateTime(LocalDateTime.now());
        save(batchLog);

        List<Map<String, Object>> details = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            Map<String, Order> orderMap = queryOrders(dto.getOrderNos());
            List<String> validOrderNos = new ArrayList<>();
            Map<String, String> failReasons = new HashMap<>();

            for (String orderNo : dto.getOrderNos()) {
                Order order = orderMap.get(orderNo);
                if (order == null) {
                    failReasons.put(orderNo, "订单不存在");
                } else if (!isValidShipStatus(order.getOrderStatus())) {
                    failReasons.put(orderNo, "订单状态不允许发货，当前状态: " + getStatusName(order.getOrderStatus()));
                } else {
                    validOrderNos.add(orderNo);
                }
            }

            log.info("批量发货预校验完成，有效订单: {}，无效订单: {}", validOrderNos.size(), failReasons.size());

            if (!validOrderNos.isEmpty()) {
                int batchNoCount = 0;
                for (int i = 0; i < validOrderNos.size(); i += BATCH_SIZE) {
                    batchNoCount++;
                    int end = Math.min(i + BATCH_SIZE, validOrderNos.size());
                    List<String> batch = validOrderNos.subList(i, end);
                    int batchSuccess = processBatchShip(batch, orderMap, dto.getRemark());
                    successCount += batchSuccess;
                    log.info("批次 {} 处理完成，成功: {}，当前累计成功: {}", batchNoCount, batchSuccess, successCount);
                }
            }

            for (Map.Entry<String, String> entry : failReasons.entrySet()) {
                BatchOperationDetail detail = createDetail(batchNo, entry.getKey(), 0, entry.getValue());
                detailMapper.insert(detail);
                details.add(buildDetailMap(detail));
                failCount++;
            }

            for (String orderNo : validOrderNos) {
                Order order = orderMap.get(orderNo);
                if (order != null && OrderStatusEnum.SHIPPED.getCode().equals(order.getOrderStatus())) {
                    BatchOperationDetail detail = createDetail(batchNo, orderNo, 1, null);
                    detailMapper.insert(detail);
                    details.add(buildDetailMap(detail));
                }
            }

        } catch (Exception e) {
            log.error("批量发货发生异常，批次号: {}", batchNo, e);
        }

        failCount = dto.getOrderNos().size() - successCount;
        updateBatchLog(batchLog, successCount, failCount, details);

        Map<String, Object> result = new HashMap<>();
        result.put("batchNo", batchNo);
        result.put("totalCount", dto.getOrderNos().size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", details);
        
        log.info("批量发货完成，批次号: {}，总计: {}，成功: {}，失败: {}", 
                batchNo, dto.getOrderNos().size(), successCount, failCount);
        return result;
    }

    private Map<String, Order> queryOrders(List<String> orderNos) {
        if (orderNos.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().in(Order::getOrderNo, orderNos)
        );
        return orders.stream().collect(Collectors.toMap(Order::getOrderNo, o -> o));
    }

    private boolean isValidShipStatus(Integer status) {
        if (status == null) return false;
        return OrderStatusEnum.PAID.getCode().equals(status) ||
               OrderStatusEnum.PENDING_SHIPMENT.getCode().equals(status);
    }

    private String getStatusName(Integer status) {
        String[] names = {"待支付", "已支付", "待发货", "已发货", "已完成", "已取消", "已退款"};
        return status != null && status < names.length ? names[status] : "未知";
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int processBatchShip(List<String> orderNos, Map<String, Order> orderMap, String remark) {
        if (orderNos.isEmpty()) return 0;

        try {
            LocalDateTime now = LocalDateTime.now();
            int updateCount = orderMapper.batchUpdateShipStatus(orderNos, OrderStatusEnum.SHIPPED.getCode(), now);

            for (String orderNo : orderNos) {
                Order order = orderMap.get(orderNo);
                if (order != null) {
                    order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
                    order.setShipTime(now);
                    order.setUpdateTime(now);
                    try {
                        orderMessageProducer.sendOrderStatusChange(
                                orderNo, order.getUserId(), OrderStatusEnum.PAID.getCode(),
                                OrderStatusEnum.SHIPPED.getCode(), "订单已发货", remark != null ? remark : "批量发货");
                    } catch (Exception e) {
                        log.warn("发送订单状态变更消息失败: {}", orderNo, e);
                    }
                }
            }

            return updateCount;
        } catch (Exception e) {
            log.error("批量更新发货状态失败，订单列表: {}", orderNos, e);
            return 0;
        }
    }

    public Map<String, Object> batchCancel(BatchOperationDTO dto) {
        String batchNo = "BATCH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("开始批量取消，批次号: {}，订单数量: {}", batchNo, dto.getOrderNos().size());

        BatchOperationLog batchLog = new BatchOperationLog();
        batchLog.setBatchNo(batchNo);
        batchLog.setOperationType("CANCEL");
        batchLog.setOperatorId(dto.getOperatorId());
        batchLog.setOperatorName(dto.getOperatorName());
        batchLog.setTotalCount(dto.getOrderNos().size());
        batchLog.setSuccessCount(0);
        batchLog.setFailCount(0);
        batchLog.setStatus(0);
        batchLog.setCreateTime(LocalDateTime.now());
        save(batchLog);

        List<Map<String, Object>> details = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            Map<String, Order> orderMap = queryOrders(dto.getOrderNos());
            List<String> validOrderNos = new ArrayList<>();
            Map<String, String> failReasons = new HashMap<>();

            for (String orderNo : dto.getOrderNos()) {
                Order order = orderMap.get(orderNo);
                if (order == null) {
                    failReasons.put(orderNo, "订单不存在");
                } else if (!isValidCancelStatus(order.getOrderStatus())) {
                    failReasons.put(orderNo, "订单状态不允许取消，当前状态: " + getStatusName(order.getOrderStatus()));
                } else {
                    validOrderNos.add(orderNo);
                }
            }

            log.info("批量取消预校验完成，有效订单: {}，无效订单: {}", validOrderNos.size(), failReasons.size());

            if (!validOrderNos.isEmpty()) {
                int batchNoCount = 0;
                for (int i = 0; i < validOrderNos.size(); i += BATCH_SIZE) {
                    batchNoCount++;
                    int end = Math.min(i + BATCH_SIZE, validOrderNos.size());
                    List<String> batch = validOrderNos.subList(i, end);
                    int batchSuccess = processBatchCancel(batch, orderMap, dto.getRemark());
                    successCount += batchSuccess;
                    log.info("批次 {} 处理完成，成功: {}，当前累计成功: {}", batchNoCount, batchSuccess, successCount);
                }
            }

            for (Map.Entry<String, String> entry : failReasons.entrySet()) {
                BatchOperationDetail detail = createDetail(batchNo, entry.getKey(), 0, entry.getValue());
                detailMapper.insert(detail);
                details.add(buildDetailMap(detail));
                failCount++;
            }

            for (String orderNo : validOrderNos) {
                Order order = orderMap.get(orderNo);
                if (order != null && OrderStatusEnum.CANCELLED.getCode().equals(order.getOrderStatus())) {
                    BatchOperationDetail detail = createDetail(batchNo, orderNo, 1, null);
                    detailMapper.insert(detail);
                    details.add(buildDetailMap(detail));
                }
            }

        } catch (Exception e) {
            log.error("批量取消发生异常，批次号: {}", batchNo, e);
        }

        failCount = dto.getOrderNos().size() - successCount;
        updateBatchLog(batchLog, successCount, failCount, details);

        Map<String, Object> result = new HashMap<>();
        result.put("batchNo", batchNo);
        result.put("totalCount", dto.getOrderNos().size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", details);
        
        log.info("批量取消完成，批次号: {}，总计: {}，成功: {}，失败: {}", 
                batchNo, dto.getOrderNos().size(), successCount, failCount);
        return result;
    }

    private boolean isValidCancelStatus(Integer status) {
        if (status == null) return false;
        return OrderStatusEnum.PENDING_PAYMENT.getCode().equals(status) ||
               OrderStatusEnum.PAID.getCode().equals(status) ||
               OrderStatusEnum.PENDING_SHIPMENT.getCode().equals(status);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int processBatchCancel(List<String> orderNos, Map<String, Order> orderMap, String remark) {
        if (orderNos.isEmpty()) return 0;

        try {
            LocalDateTime now = LocalDateTime.now();
            int updateCount = orderMapper.batchUpdateCancelStatus(orderNos, OrderStatusEnum.CANCELLED.getCode(), now);

            for (String orderNo : orderNos) {
                Order order = orderMap.get(orderNo);
                if (order != null) {
                    Integer previousStatus = order.getOrderStatus();
                    order.setOrderStatus(OrderStatusEnum.CANCELLED.getCode());
                    order.setCancelTime(now);
                    order.setUpdateTime(now);
                    try {
                        orderMessageProducer.sendOrderStatusChange(
                                orderNo, order.getUserId(), previousStatus,
                                OrderStatusEnum.CANCELLED.getCode(), "订单已取消", remark != null ? remark : "批量取消");
                    } catch (Exception e) {
                        log.warn("发送订单状态变更消息失败: {}", orderNo, e);
                    }
                }
            }

            return updateCount;
        } catch (Exception e) {
            log.error("批量更新取消状态失败，订单列表: {}", orderNos, e);
            return 0;
        }
    }

    private BatchOperationDetail createDetail(String batchNo, String orderNo, Integer success, String errorMsg) {
        BatchOperationDetail detail = new BatchOperationDetail();
        detail.setBatchNo(batchNo);
        detail.setOrderNo(orderNo);
        detail.setSuccess(success);
        detail.setErrorMsg(errorMsg);
        detail.setCreateTime(LocalDateTime.now());
        return detail;
    }

    private Map<String, Object> buildDetailMap(BatchOperationDetail detail) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", detail.getOrderNo());
        map.put("success", detail.getSuccess());
        map.put("errorMsg", detail.getErrorMsg());
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBatchLog(BatchOperationLog log, int successCount, int failCount, List<Map<String, Object>> details) {
        log.setSuccessCount(successCount);
        log.setFailCount(failCount);
        log.setStatus(1);
        log.setUpdateTime(LocalDateTime.now());
        try {
            log.setDetail(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.error("序列化详情失败", e);
        }
        updateById(log);
    }

    public List<BatchOperationLog> getBatchLogs(String operationType) {
        LambdaQueryWrapper<BatchOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(BatchOperationLog::getOperationType, operationType);
        }
        wrapper.orderByDesc(BatchOperationLog::getCreateTime);
        return list(wrapper);
    }

    public List<BatchOperationDetail> getBatchDetails(String batchNo) {
        return detailMapper.selectList(new LambdaQueryWrapper<BatchOperationDetail>()
                .eq(BatchOperationDetail::getBatchNo, batchNo));
    }
}
