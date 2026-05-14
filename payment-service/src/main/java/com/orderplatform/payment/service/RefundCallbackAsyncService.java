package com.orderplatform.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.enums.CallbackStatusEnum;
import com.orderplatform.common.enums.CallbackTypeEnum;
import com.orderplatform.common.enums.RefundStatusEnum;
import com.orderplatform.payment.entity.RefundRecord;
import com.orderplatform.payment.mapper.RefundRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class RefundCallbackAsyncService {

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private CallbackLogService callbackLogService;

    @Autowired
    private MockPaymentGatewayService mockPaymentGatewayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("paymentCallbackExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Void> processRefundCallbackAsync(String refundNo) {
        log.info("开始异步处理退款回调, refundNo: {}", refundNo);

        RefundRecord refundRecord = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getRefundNo, refundNo)
        );

        if (refundRecord == null) {
            log.error("退款记录不存在, refundNo: {}", refundNo);
            return CompletableFuture.completedFuture(null);
        }

        if (!RefundStatusEnum.REFUNDING.getCode().equals(refundRecord.getRefundStatus())) {
            log.warn("退款状态不是退款中, 跳过回调, refundNo: {}, status: {}", 
                    refundNo, refundRecord.getRefundStatus());
            return CompletableFuture.completedFuture(null);
        }

        try {
            Map<String, Object> gatewayResult = mockPaymentGatewayService.processRefund(
                    refundNo, true
            );

            String idempotentKey = "REFUND_CALLBACK_" + refundNo + "_" + 
                    System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            String callbackNo = callbackLogService.createCallbackLog(
                    CallbackTypeEnum.REFUND_CALLBACK,
                    refundNo,
                    refundRecord.getOrderNo(),
                    objectMapper.writeValueAsString(gatewayResult)
            );

            boolean success = (Boolean) gatewayResult.get("success");
            String thirdPartyRefundNo = (String) gatewayResult.get("thirdPartyRefundNo");

            if (success) {
                refundRecord.setRefundStatus(RefundStatusEnum.REFUND_SUCCESS.getCode());
                refundRecord.setRefundTime(LocalDateTime.now());
                refundRecord.setThirdPartyRefundNo(thirdPartyRefundNo);
                refundRecord.setCallbackCount(refundRecord.getCallbackCount() + 1);
                refundRecord.setLastCallbackTime(LocalDateTime.now());
                refundRecord.setUpdateTime(LocalDateTime.now());
                refundRecordMapper.updateById(refundRecord);

                callbackLogService.updateCallbackSuccess(callbackNo, "success");
                log.info("退款回调处理成功, refundNo: {}", refundNo);
            } else {
                refundRecord.setRefundStatus(RefundStatusEnum.REFUND_FAILED.getCode());
                refundRecord.setCallbackCount(refundRecord.getCallbackCount() + 1);
                refundRecord.setLastCallbackTime(LocalDateTime.now());
                refundRecord.setUpdateTime(LocalDateTime.now());
                refundRecordMapper.updateById(refundRecord);

                callbackLogService.updateCallbackSuccess(callbackNo, "failed");
                log.info("退款回调处理失败, refundNo: {}", refundNo);
            }

        } catch (Exception e) {
            log.error("异步处理退款回调异常, refundNo: {}", refundNo, e);
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async("paymentCallbackExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Void> retryFailedCallback(String callbackNo) {
        log.info("重试失败回调, callbackNo: {}", callbackNo);
        return CompletableFuture.completedFuture(null);
    }
}
