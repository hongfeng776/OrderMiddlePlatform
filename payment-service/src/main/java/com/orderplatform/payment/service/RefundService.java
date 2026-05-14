package com.orderplatform.payment.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.enums.CallbackTypeEnum;
import com.orderplatform.common.enums.PayStatusEnum;
import com.orderplatform.common.enums.RefundStatusEnum;
import com.orderplatform.payment.dto.RefundApplyDTO;
import com.orderplatform.payment.entity.PaymentRecord;
import com.orderplatform.payment.entity.RefundRecord;
import com.orderplatform.payment.exception.RefundException;
import com.orderplatform.payment.mapper.RefundRecordMapper;
import com.orderplatform.payment.vo.RefundExportVO;
import com.orderplatform.payment.vo.RefundProgressVO;
import com.orderplatform.payment.vo.RefundReasonStatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RefundService extends ServiceImpl<RefundRecordMapper, RefundRecord> {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MockPaymentGatewayService mockPaymentGatewayService;

    @Autowired
    private CallbackLogService callbackLogService;

    @Autowired
    private RefundCallbackAsyncService refundCallbackAsyncService;

    @Autowired
    private com.orderplatform.common.feign.OrderFeignClient orderFeignClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<Integer> ALLOW_REFUND_ORDER_STATUSES = List.of(1, 2, 3, 4);

    @Transactional(rollbackFor = Exception.class)
    public String applyRefund(RefundApplyDTO dto) {
        log.info("收到退款申请, orderNo: {}, payNo: {}, userId: {}", 
                dto.getOrderNo(), dto.getPayNo(), dto.getUserId());

        validateRefundApply(dto);

        String idempotentKey = generateIdempotentKey(dto);

        RefundRecord existingRefund = getOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getIdempotentKey, idempotentKey));
        if (existingRefund != null) {
            log.warn("重复的退款申请, 返回已有的退款单号: {}", existingRefund.getRefundNo());
            return existingRefund.getRefundNo();
        }

        String refundNo = "REFUND" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setRefundNo(refundNo);
        refundRecord.setOrderNo(dto.getOrderNo());
        refundRecord.setPayNo(dto.getPayNo());
        refundRecord.setUserId(dto.getUserId());
        refundRecord.setRefundAmount(dto.getRefundAmount());
        refundRecord.setRefundReason(dto.getRefundReason());
        refundRecord.setRefundVoucher(dto.getRefundVouchers() != null ? 
                String.join(",", dto.getRefundVouchers()) : null);
        refundRecord.setRefundStatus(RefundStatusEnum.PENDING_AUDIT.getCode());
        refundRecord.setIdempotentKey(idempotentKey);
        refundRecord.setCallbackCount(0);
        refundRecord.setCreateTime(LocalDateTime.now());
        refundRecord.setUpdateTime(LocalDateTime.now());

        try {
            save(refundRecord);
            log.info("退款申请创建成功, refundNo: {}", refundNo);
        } catch (DuplicateKeyException e) {
            log.warn("退款申请幂等冲突, 重新查询: {}", idempotentKey);
            RefundRecord dupRefund = getOne(new LambdaQueryWrapper<RefundRecord>()
                    .eq(RefundRecord::getIdempotentKey, idempotentKey));
            if (dupRefund != null) {
                return dupRefund.getRefundNo();
            }
            throw new RefundException("REFUND_CREATE_FAILED", "创建退款记录失败");
        }

        return refundNo;
    }

    private void validateRefundApply(RefundApplyDTO dto) {
        if (dto.getUserId() == null) {
            throw new RefundException("USER_ID_REQUIRED", "用户ID不能为空");
        }

        if (!StringUtils.hasText(dto.getOrderNo())) {
            throw new RefundException("ORDER_NO_REQUIRED", "订单号不能为空");
        }

        if (!StringUtils.hasText(dto.getPayNo())) {
            throw new RefundException("PAY_NO_REQUIRED", "支付单号不能为空");
        }

        if (dto.getRefundAmount() == null || dto.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RefundException("REFUND_AMOUNT_INVALID", "退款金额必须大于0");
        }

        PaymentRecord paymentRecord = paymentService.getOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPayNo, dto.getPayNo())
                .eq(PaymentRecord::getOrderNo, dto.getOrderNo()));

        if (paymentRecord == null) {
            throw new RefundException("PAYMENT_NOT_FOUND", "支付记录不存在");
        }

        if (!paymentRecord.getUserId().equals(dto.getUserId())) {
            throw new RefundException("PERMISSION_DENIED", "无权申请该支付的退款");
        }

        if (!PayStatusEnum.SUCCESS.getCode().equals(paymentRecord.getPayStatus())) {
            throw new RefundException("PAYMENT_STATUS_INVALID", "只有支付成功的订单才能申请退款");
        }

        if (dto.getRefundAmount().compareTo(paymentRecord.getPayAmount()) > 0) {
            throw new RefundException("REFUND_AMOUNT_EXCEED", "退款金额不能大于支付金额");
        }

        List<RefundRecord> existingRefunds = list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getPayNo, dto.getPayNo())
                .in(RefundRecord::getRefundStatus,
                        RefundStatusEnum.PENDING_AUDIT.getCode(),
                        RefundStatusEnum.AUDIT_PASS.getCode(),
                        RefundStatusEnum.REFUNDING.getCode(),
                        RefundStatusEnum.REFUND_SUCCESS.getCode()));

        BigDecimal totalRefunded = existingRefunds.stream()
                .map(RefundRecord::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRefunded.add(dto.getRefundAmount()).compareTo(paymentRecord.getPayAmount()) > 0) {
            throw new RefundException("TOTAL_REFUND_EXCEED", "退款总金额不能大于支付金额");
        }
    }

    private String generateIdempotentKey(RefundApplyDTO dto) {
        String raw = dto.getOrderNo() + "_" + dto.getPayNo() + "_" + 
                dto.getUserId() + "_" + dto.getRefundAmount() + "_" + 
                (dto.getRefundReason() != null ? dto.getRefundReason() : "");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "REFUND_IDEMPOTENT_" + sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "REFUND_IDEMPOTENT_" + UUID.randomUUID().toString();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean auditRefund(String refundNo, Long auditUserId, boolean pass, String auditRemark) {
        log.info("审核退款申请, refundNo: {}, auditUserId: {}, pass: {}", refundNo, auditUserId, pass);

        RefundRecord refundRecord = getOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo));

        if (refundRecord == null) {
            throw new RefundException("REFUND_NOT_FOUND", "退款记录不存在");
        }

        if (!RefundStatusEnum.PENDING_AUDIT.getCode().equals(refundRecord.getRefundStatus())) {
            throw new RefundException("REFUND_STATUS_INVALID", "当前退款状态不允许审核");
        }

        if (auditUserId == null) {
            throw new RefundException("AUDIT_USER_REQUIRED", "审核人ID不能为空");
        }

        if (pass) {
            refundRecord.setRefundStatus(RefundStatusEnum.AUDIT_PASS.getCode());
            refundRecord.setAuditUserId(auditUserId);
            refundRecord.setAuditTime(LocalDateTime.now());
            refundRecord.setAuditRemark(auditRemark);
            refundRecord.setUpdateTime(LocalDateTime.now());
            updateById(refundRecord);

            processRefund(refundNo);
        } else {
            refundRecord.setRefundStatus(RefundStatusEnum.AUDIT_REJECT.getCode());
            refundRecord.setAuditUserId(auditUserId);
            refundRecord.setAuditTime(LocalDateTime.now());
            refundRecord.setAuditRemark(auditRemark);
            refundRecord.setUpdateTime(LocalDateTime.now());
            updateById(refundRecord);
        }

        log.info("退款审核完成, refundNo: {}, pass: {}", refundNo, pass);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processRefund(String refundNo) {
        log.info("开始处理退款, refundNo: {}", refundNo);

        RefundRecord refundRecord = getOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo));

        if (refundRecord == null) {
            throw new RefundException("REFUND_NOT_FOUND", "退款记录不存在");
        }

        if (RefundStatusEnum.REFUNDING.getCode().equals(refundRecord.getRefundStatus())
                || RefundStatusEnum.REFUND_SUCCESS.getCode().equals(refundRecord.getRefundStatus())
                || RefundStatusEnum.REFUND_FAILED.getCode().equals(refundRecord.getRefundStatus())) {
            log.warn("退款已在处理中或已完成, 跳过重复处理, refundNo: {}, status: {}", 
                    refundNo, refundRecord.getRefundStatus());
            return;
        }

        if (!RefundStatusEnum.AUDIT_PASS.getCode().equals(refundRecord.getRefundStatus())) {
            throw new RefundException("REFUND_STATUS_INVALID", "退款状态不是审核通过");
        }

        refundRecord.setRefundStatus(RefundStatusEnum.REFUNDING.getCode());
        refundRecord.setUpdateTime(LocalDateTime.now());
        updateById(refundRecord);

        Map<String, Object> gatewayResult = mockPaymentGatewayService.createRefund(
                refundNo,
                refundRecord.getPayNo(),
                refundRecord.getRefundAmount()
        );

        refundRecord.setThirdPartyRefundNo((String) gatewayResult.get("thirdPartyRefundNo"));
        refundRecord.setUpdateTime(LocalDateTime.now());
        updateById(refundRecord);

        refundCallbackAsyncService.processRefundCallbackAsync(refundNo);

        log.info("退款处理已提交异步回调, refundNo: {}", refundNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleRefundCallback(Map<String, Object> callbackData) {
        String refundNo = (String) callbackData.get("refundNo");
        Boolean success = (Boolean) callbackData.get("success");
        String thirdPartyRefundNo = (String) callbackData.get("thirdPartyRefundNo");

        log.info("收到退款回调, refundNo: {}, success: {}", refundNo, success);

        String callbackNo = callbackLogService.createCallbackLog(
                CallbackTypeEnum.REFUND_CALLBACK,
                refundNo,
                null,
                objectMapper.valueToTree(callbackData).toString()
        );

        try {
            RefundRecord refundRecord = getOne(new LambdaQueryWrapper<RefundRecord>()
                    .eq(RefundRecord::getRefundNo, refundNo));

            if (refundRecord == null) {
                throw new RefundException("REFUND_NOT_FOUND", "退款记录不存在");
            }

            if (RefundStatusEnum.REFUND_SUCCESS.getCode().equals(refundRecord.getRefundStatus())) {
                log.warn("退款已成功, 忽略重复回调, refundNo: {}", refundNo);
                callbackLogService.updateCallbackSuccess(callbackNo, "duplicate_callback_ignored");
                return;
            }

            if (success) {
                refundRecord.setRefundStatus(RefundStatusEnum.REFUND_SUCCESS.getCode());
                refundRecord.setRefundTime(LocalDateTime.now());
            } else {
                refundRecord.setRefundStatus(RefundStatusEnum.REFUND_FAILED.getCode());
            }

            refundRecord.setThirdPartyRefundNo(thirdPartyRefundNo);
            refundRecord.setCallbackCount(refundRecord.getCallbackCount() + 1);
            refundRecord.setLastCallbackTime(LocalDateTime.now());
            refundRecord.setUpdateTime(LocalDateTime.now());
            updateById(refundRecord);

            if (success) {
                try {
                    Map<String, String> remarkMap = new HashMap<>();
                    remarkMap.put("remark", "退款成功");
                    orderFeignClient.refundSuccess(refundRecord.getOrderNo(), remarkMap);
                    log.info("订单状态更新为已退款, orderNo: {}", refundRecord.getOrderNo());
                } catch (Exception e) {
                    log.error("更新订单状态失败, orderNo: {}", refundRecord.getOrderNo(), e);
                }
            }

            callbackLogService.updateCallbackSuccess(callbackNo, "success");
            log.info("退款回调处理成功, refundNo: {}", refundNo);

        } catch (Exception e) {
            log.error("处理退款回调失败: {}", e.getMessage(), e);
            callbackLogService.updateCallbackFailed(callbackNo, e.getMessage());
            throw e;
        }
    }

    public RefundProgressVO getRefundProgress(String refundNo, Long userId) {
        RefundRecord refundRecord = getOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo));

        if (refundRecord == null) {
            throw new RefundException("REFUND_NOT_FOUND", "退款记录不存在");
        }

        if (!refundRecord.getUserId().equals(userId)) {
            throw new RefundException("PERMISSION_DENIED", "无权查看该退款进度");
        }

        RefundProgressVO vo = new RefundProgressVO();
        vo.setRefundNo(refundRecord.getRefundNo());
        vo.setOrderNo(refundRecord.getOrderNo());
        vo.setPayNo(refundRecord.getPayNo());
        vo.setRefundAmount(refundRecord.getRefundAmount());
        vo.setRefundReason(refundRecord.getRefundReason());
        vo.setRefundVouchers(refundRecord.getVoucherList());
        vo.setRefundStatus(refundRecord.getRefundStatus());
        vo.setRefundStatusDesc(getRefundStatusDesc(refundRecord.getRefundStatus()));
        vo.setAuditRemark(refundRecord.getAuditRemark());
        vo.setAuditTime(refundRecord.getAuditTime());
        vo.setRefundTime(refundRecord.getRefundTime());
        vo.setCreateTime(refundRecord.getCreateTime());

        List<RefundProgressVO.ProgressStep> steps = buildProgressSteps(refundRecord);
        vo.setProgressSteps(steps);

        return vo;
    }

    private List<RefundProgressVO.ProgressStep> buildProgressSteps(RefundRecord refundRecord) {
        List<RefundProgressVO.ProgressStep> steps = new ArrayList<>();

        RefundProgressVO.ProgressStep step1 = new RefundProgressVO.ProgressStep();
        step1.setStepName("提交退款申请");
        step1.setStatus("completed");
        step1.setTime(refundRecord.getCreateTime());
        steps.add(step1);

        if (refundRecord.getRefundStatus() >= RefundStatusEnum.AUDIT_PASS.getCode()) {
            RefundProgressVO.ProgressStep step2 = new RefundProgressVO.ProgressStep();
            step2.setStepName("审核通过");
            step2.setStatus("completed");
            step2.setTime(refundRecord.getAuditTime());
            step2.setRemark(refundRecord.getAuditRemark());
            steps.add(step2);
        } else if (refundRecord.getRefundStatus().equals(RefundStatusEnum.AUDIT_REJECT.getCode())) {
            RefundProgressVO.ProgressStep step2 = new RefundProgressVO.ProgressStep();
            step2.setStepName("审核拒绝");
            step2.setStatus("failed");
            step2.setTime(refundRecord.getAuditTime());
            step2.setRemark(refundRecord.getAuditRemark());
            steps.add(step2);
        } else {
            RefundProgressVO.ProgressStep step2 = new RefundProgressVO.ProgressStep();
            step2.setStepName("等待审核");
            step2.setStatus("processing");
            steps.add(step2);
        }

        if (refundRecord.getRefundStatus() >= RefundStatusEnum.REFUNDING.getCode()) {
            RefundProgressVO.ProgressStep step3 = new RefundProgressVO.ProgressStep();
            step3.setStepName("退款处理中");
            step3.setStatus(refundRecord.getRefundStatus() >= RefundStatusEnum.REFUND_SUCCESS.getCode() 
                    ? "completed" : "processing");
            steps.add(step3);
        }

        if (refundRecord.getRefundStatus().equals(RefundStatusEnum.REFUND_SUCCESS.getCode())) {
            RefundProgressVO.ProgressStep step4 = new RefundProgressVO.ProgressStep();
            step4.setStepName("退款成功");
            step4.setStatus("completed");
            step4.setTime(refundRecord.getRefundTime());
            steps.add(step4);
        } else if (refundRecord.getRefundStatus().equals(RefundStatusEnum.REFUND_FAILED.getCode())) {
            RefundProgressVO.ProgressStep step4 = new RefundProgressVO.ProgressStep();
            step4.setStepName("退款失败");
            step4.setStatus("failed");
            steps.add(step4);
        }

        return steps;
    }

    private String getRefundStatusDesc(Integer status) {
        RefundStatusEnum statusEnum = RefundStatusEnum.getByCode(status);
        return statusEnum != null ? statusEnum.getDesc() : "未知状态";
    }

    public List<RefundRecord> getUserRefundList(Long userId) {
        return list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getUserId, userId)
                .orderByDesc(RefundRecord::getCreateTime));
    }

    public List<RefundRecord> getPendingAuditList() {
        return list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundStatus, RefundStatusEnum.PENDING_AUDIT.getCode())
                .orderByAsc(RefundRecord::getCreateTime));
    }

    public void exportRefundList(Long userId, HttpServletResponse response) throws IOException {
        log.info("开始导出退款记录，userId: {}", userId);
        
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(RefundRecord::getUserId, userId);
        }
        wrapper.orderByDesc(RefundRecord::getCreateTime);
        
        List<RefundRecord> refundList = list(wrapper);
        List<RefundExportVO> exportList = refundList.stream().map(refund -> {
            RefundExportVO vo = new RefundExportVO();
            vo.setRefundNo(refund.getRefundNo());
            vo.setOrderNo(refund.getOrderNo());
            vo.setPayNo(refund.getPayNo());
            vo.setUserId(refund.getUserId());
            vo.setRefundAmount(refund.getRefundAmount());
            vo.setRefundReason(refund.getRefundReason() != null ? refund.getRefundReason() : "");
            vo.setRefundStatusText(getRefundStatusDesc(refund.getRefundStatus()));
            vo.setAuditRemark(refund.getAuditRemark() != null ? refund.getAuditRemark() : "");
            vo.setRefundTime(refund.getRefundTime() != null ? 
                    refund.getRefundTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            vo.setCreateTime(refund.getCreateTime() != null ? 
                    refund.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            return vo;
        }).collect(Collectors.toList());

        String fileName = "退款记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=\"" + encodedFileName + ".xlsx\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), RefundExportVO.class)
                .registerWriteHandler(new com.alibaba.excel.write.handler.SheetWriteHandler() {
                    @Override
                    public void afterSheetCreate(com.alibaba.excel.write.handler.context.SheetWriteHandlerContext context) {
                    }
                })
                .sheet("退款记录")
                .doWrite(exportList);

        log.info("退款记录导出完成，共导出{}条记录", exportList.size());
    }

    public List<RefundReasonStatVO> getRefundReasonStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("开始统计退款原因，startTime: {}, endTime: {}", startTime, endTime);

        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(RefundRecord::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(RefundRecord::getCreateTime, endTime);
        }
        wrapper.isNotNull(RefundRecord::getRefundReason);
        wrapper.ne(RefundRecord::getRefundReason, "");

        List<RefundRecord> refundList = list(wrapper);
        
        if (refundList.isEmpty()) {
            return new ArrayList<>();
        }

        long totalCount = refundList.size();
        BigDecimal totalAmount = refundList.stream()
                .map(RefundRecord::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<RefundRecord>> groupByReason = refundList.stream()
                .collect(Collectors.groupingBy(RefundRecord::getRefundReason));

        List<RefundReasonStatVO> result = new ArrayList<>();
        for (Map.Entry<String, List<RefundRecord>> entry : groupByReason.entrySet()) {
            RefundReasonStatVO stat = new RefundReasonStatVO();
            stat.setRefundReason(entry.getKey());
            stat.setCount((long) entry.getValue().size());
            stat.setTotalAmount(entry.getValue().stream()
                    .map(RefundRecord::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            
            BigDecimal percentage = totalCount > 0 ? 
                    BigDecimal.valueOf(entry.getValue().size())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            stat.setPercentage(percentage + "%");
            
            result.add(stat);
        }

        result.sort((a, b) -> b.getCount().compareTo(a.getCount()));
        log.info("退款原因统计完成，共{}种原因", result.size());
        return result;
    }
}
