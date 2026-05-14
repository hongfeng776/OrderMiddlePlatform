package com.orderplatform.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.payment.entity.PaymentRecord;
import com.orderplatform.payment.mapper.PaymentRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class PaymentService extends ServiceImpl<PaymentRecordMapper, PaymentRecord> {

    public String createPayment(String orderNo, Long userId, java.math.BigDecimal amount, Integer payType) {
        String payNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        PaymentRecord payment = new PaymentRecord();
        payment.setPayNo(payNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setPayAmount(amount);
        payment.setPayType(payType);
        payment.setPayStatus(0);
        payment.setCreateTime(java.time.LocalDateTime.now());
        payment.setUpdateTime(java.time.LocalDateTime.now());
        save(payment);
        
        return payNo;
    }

    public boolean processPayment(String payNo) {
        PaymentRecord payment = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPayNo, payNo));
        
        if (payment != null && payment.getPayStatus() == 0) {
            payment.setPayStatus(1);
            payment.setPayTime(java.time.LocalDateTime.now());
            payment.setThirdPartyNo("THIRD" + System.currentTimeMillis());
            updateById(payment);
            return true;
        }
        return false;
    }
}
