package com.orderplatform.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MockPaymentGatewayService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> createPayment(String orderNo, String payNo, java.math.BigDecimal amount, Integer payType) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payNo", payNo);
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("payType", payType);
        result.put("gatewayOrderNo", "GATEWAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("payUrl", "http://localhost:8080/mock-pay/" + payNo);
        result.put("expireTime", LocalDateTime.now().plusMinutes(30).toString());
        return result;
    }

    public Map<String, Object> processPay(String payNo, boolean success) {
        Map<String, Object> result = new HashMap<>();
        result.put("payNo", payNo);
        result.put("success", success);
        result.put("thirdPartyNo", "THIRD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("payTime", LocalDateTime.now().toString());
        if (success) {
            result.put("message", "支付成功");
        } else {
            result.put("message", "支付失败");
            result.put("errorCode", "PAY_FAILED");
            result.put("errorMsg", "余额不足");
        }
        return result;
    }

    public Map<String, Object> createRefund(String refundNo, String payNo, java.math.BigDecimal refundAmount) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("refundNo", refundNo);
        result.put("payNo", payNo);
        result.put("refundAmount", refundAmount);
        result.put("thirdPartyRefundNo", "REFUND" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("refundTime", LocalDateTime.now().toString());
        return result;
    }

    public Map<String, Object> processRefund(String refundNo, boolean success) {
        Map<String, Object> result = new HashMap<>();
        result.put("refundNo", refundNo);
        result.put("success", success);
        result.put("thirdPartyRefundNo", "THIRD_REFUND" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("refundTime", LocalDateTime.now().toString());
        if (success) {
            result.put("message", "退款成功");
        } else {
            result.put("message", "退款失败");
            result.put("errorCode", "REFUND_FAILED");
            result.put("errorMsg", "退款账户异常");
        }
        return result;
    }
}
