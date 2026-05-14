package com.orderplatform.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.enums.CallbackTypeEnum;
import com.orderplatform.common.enums.PayStatusEnum;
import com.orderplatform.payment.entity.PaymentRecord;
import com.orderplatform.payment.mapper.PaymentRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService extends ServiceImpl<PaymentRecordMapper, PaymentRecord> {

    @Autowired
    private MockPaymentGatewayService mockPaymentGatewayService;

    @Autowired
    private CallbackLogService callbackLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createPayment(String orderNo, Long userId, java.math.BigDecimal amount, Integer payType) {
        String payNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        PaymentRecord payment = new PaymentRecord();
        payment.setPayNo(payNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setPayAmount(amount);
        payment.setPayType(payType);
        payment.setPayStatus(PayStatusEnum.PENDING.getCode());
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        save(payment);

        mockPaymentGatewayService.createPayment(orderNo, payNo, amount, payType);
        
        return payNo;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processPayment(String payNo, boolean success) {
        PaymentRecord payment = getOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPayNo, payNo));
        
        if (payment != null && PayStatusEnum.PENDING.getCode().equals(payment.getPayStatus())) {
            Map<String, Object> gatewayResult = mockPaymentGatewayService.processPay(payNo, success);
            
            if (success) {
                payment.setPayStatus(PayStatusEnum.SUCCESS.getCode());
                payment.setPayTime(LocalDateTime.now());
                payment.setThirdPartyNo((String) gatewayResult.get("thirdPartyNo"));
            } else {
                payment.setPayStatus(PayStatusEnum.FAILED.getCode());
            }
            payment.setUpdateTime(LocalDateTime.now());
            updateById(payment);
            return true;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(Map<String, Object> callbackData) {
        String payNo = (String) callbackData.get("payNo");
        Boolean success = (Boolean) callbackData.get("success");
        String thirdPartyNo = (String) callbackData.get("thirdPartyNo");

        String callbackNo = callbackLogService.createCallbackLog(
                CallbackTypeEnum.PAYMENT_CALLBACK,
                payNo,
                null,
                objectMapper.valueToTree(callbackData).toString()
        );

        try {
            PaymentRecord payment = getOne(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getPayNo, payNo));

            if (payment == null) {
                throw new RuntimeException("支付记录不存在");
            }

            if (success) {
                payment.setPayStatus(PayStatusEnum.SUCCESS.getCode());
                payment.setPayTime(LocalDateTime.now());
            } else {
                payment.setPayStatus(PayStatusEnum.FAILED.getCode());
            }

            payment.setThirdPartyNo(thirdPartyNo);
            payment.setUpdateTime(LocalDateTime.now());
            updateById(payment);

            callbackLogService.updateCallbackSuccess(callbackNo, "success");

        } catch (Exception e) {
            log.error("处理支付回调失败: {}", e.getMessage(), e);
            callbackLogService.updateCallbackFailed(callbackNo, e.getMessage());
            throw e;
        }
    }

    public PaymentRecord getPaymentByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, orderNo)
                .orderByDesc(PaymentRecord::getCreateTime)
                .last("LIMIT 1"));
    }

    public List<PaymentRecord> getUserPaymentList(Long userId) {
        return list(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getUserId, userId)
                .orderByDesc(PaymentRecord::getCreateTime));
    }
}
