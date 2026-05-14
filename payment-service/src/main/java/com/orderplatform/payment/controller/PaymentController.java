package com.orderplatform.payment.controller;

import com.orderplatform.common.result.Result;
import com.orderplatform.payment.dto.RefundApplyDTO;
import com.orderplatform.payment.entity.CallbackLog;
import com.orderplatform.payment.entity.PaymentRecord;
import com.orderplatform.payment.entity.RefundRecord;
import com.orderplatform.payment.service.CallbackLogService;
import com.orderplatform.payment.service.MockPaymentGatewayService;
import com.orderplatform.payment.service.PaymentService;
import com.orderplatform.payment.service.RefundService;
import com.orderplatform.payment.service.SystemConfigService;
import com.orderplatform.payment.vo.RefundProgressVO;
import com.orderplatform.payment.vo.RefundReasonStatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private MockPaymentGatewayService mockPaymentGatewayService;

    @Autowired
    private CallbackLogService callbackLogService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private com.orderplatform.payment.task.RefundTimeoutTask refundTimeoutTask;

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
    public Result<Boolean> processPayment(@PathVariable String payNo, @RequestParam boolean success) {
        boolean result = paymentService.processPayment(payNo, success);
        return Result.success(result);
    }

    @PostMapping("/callback/payment")
    public Result<String> paymentCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("收到支付回调: {}", callbackData);
        paymentService.handlePaymentCallback(callbackData);
        return Result.success("success");
    }

    @PostMapping("/callback/refund")
    public Result<String> refundCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("收到退款回调: {}", callbackData);
        refundService.handleRefundCallback(callbackData);
        return Result.success("success");
    }

    @GetMapping("/order/{orderNo}")
    public Result<PaymentRecord> getPaymentByOrderNo(@PathVariable String orderNo) {
        PaymentRecord payment = paymentService.getPaymentByOrderNo(orderNo);
        return Result.success(payment);
    }

    @GetMapping("/user/{userId}")
    public Result<List<PaymentRecord>> getUserPaymentList(@PathVariable Long userId) {
        List<PaymentRecord> list = paymentService.getUserPaymentList(userId);
        return Result.success(list);
    }

    @PostMapping("/refund/apply")
    public Result<String> applyRefund(@RequestBody RefundApplyDTO dto) {
        String refundNo = refundService.applyRefund(dto);
        return Result.success(refundNo);
    }

    @GetMapping("/refund/progress/{refundNo}")
    public Result<RefundProgressVO> getRefundProgress(@PathVariable String refundNo, @RequestParam Long userId) {
        RefundProgressVO progress = refundService.getRefundProgress(refundNo, userId);
        return Result.success(progress);
    }

    @PostMapping("/refund/audit")
    public Result<Boolean> auditRefund(@RequestBody Map<String, Object> params) {
        String refundNo = (String) params.get("refundNo");
        Long auditUserId = Long.valueOf(params.get("auditUserId").toString());
        boolean pass = (Boolean) params.get("pass");
        String auditRemark = (String) params.get("auditRemark");
        
        boolean result = refundService.auditRefund(refundNo, auditUserId, pass, auditRemark);
        return Result.success(result);
    }

    @GetMapping("/refund/user/{userId}")
    public Result<List<RefundRecord>> getUserRefundList(@PathVariable Long userId) {
        List<RefundRecord> list = refundService.getUserRefundList(userId);
        return Result.success(list);
    }

    @GetMapping("/refund/pending-audit")
    public Result<List<RefundRecord>> getPendingAuditList() {
        List<RefundRecord> list = refundService.getPendingAuditList();
        return Result.success(list);
    }

    @GetMapping("/refund/{refundNo}")
    public Result<RefundRecord> getRefundDetail(@PathVariable String refundNo) {
        RefundRecord refund = refundService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo));
        return Result.success(refund);
    }

    @GetMapping("/callback/list")
    public Result<List<CallbackLog>> getCallbackList(@RequestParam(required = false) Integer callbackType) {
        List<CallbackLog> list = callbackLogService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CallbackLog>()
                .eq(callbackType != null, CallbackLog::getCallbackType, callbackType)
                .orderByDesc(CallbackLog::getCreateTime));
        return Result.success(list);
    }

    @PostMapping("/mock/pay-success")
    public Result<Map<String, Object>> mockPaySuccess(@RequestParam String payNo) {
        Map<String, Object> result = mockPaymentGatewayService.processPay(payNo, true);
        paymentService.handlePaymentCallback(result);
        return Result.success(result);
    }

    @PostMapping("/mock/pay-fail")
    public Result<Map<String, Object>> mockPayFail(@RequestParam String payNo) {
        Map<String, Object> result = mockPaymentGatewayService.processPay(payNo, false);
        paymentService.handlePaymentCallback(result);
        return Result.success(result);
    }

    @PostMapping("/mock/refund-success")
    public Result<Map<String, Object>> mockRefundSuccess(@RequestParam String refundNo) {
        Map<String, Object> result = mockPaymentGatewayService.processRefund(refundNo, true);
        refundService.handleRefundCallback(result);
        return Result.success(result);
    }

    @PostMapping("/mock/refund-fail")
    public Result<Map<String, Object>> mockRefundFail(@RequestParam String refundNo) {
        Map<String, Object> result = mockPaymentGatewayService.processRefund(refundNo, false);
        refundService.handleRefundCallback(result);
        return Result.success(result);
    }

    @GetMapping("/refund/export")
    public void exportRefundList(@RequestParam(required = false) Long userId, HttpServletResponse response) throws IOException {
        refundService.exportRefundList(userId, response);
    }

    @GetMapping("/refund/statistics/reason")
    public Result<List<RefundReasonStatVO>> getRefundReasonStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<RefundReasonStatVO> statistics = refundService.getRefundReasonStatistics(startTime, endTime);
        return Result.success(statistics);
    }

    @GetMapping("/config/list")
    public Result<Map<String, String>> getConfigList() {
        Map<String, String> configMap = systemConfigService.getAllConfigMap();
        return Result.success(configMap);
    }

    @PostMapping("/config/update")
    public Result<Void> updateConfig(@RequestBody Map<String, String> params) {
        String configKey = params.get("configKey");
        String configValue = params.get("configValue");
        String configDesc = params.get("configDesc");
        systemConfigService.updateConfigValue(configKey, configValue, configDesc);
        return Result.success();
    }

    @PostMapping("/refund/process-timeout")
    public Result<Void> processTimeoutRefund() {
        refundTimeoutTask.processTimeoutRefund();
        return Result.success();
    }
}
