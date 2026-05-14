package com.orderplatform.payment.controller;

import com.orderplatform.common.result.Result;
import com.orderplatform.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    public Result<String> createPayment(@RequestBody Map<String, Object> params) {
        String orderNo = (String) params.get("orderNo");
        Long userId = Long.valueOf(params.get("userId").toString());
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Integer payType = Integer.valueOf(params.get("payType").toString());
        
        String payNo = paymentService.createPayment(orderNo, userId, amount, payType);
        return Result.success(payNo);
    }

    @PostMapping("/process/{payNo}")
    public Result<Boolean> processPayment(@PathVariable String payNo) {
        boolean result = paymentService.processPayment(payNo);
        return Result.success(result);
    }
}
