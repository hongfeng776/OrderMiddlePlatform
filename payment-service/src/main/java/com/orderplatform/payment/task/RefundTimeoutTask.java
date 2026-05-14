package com.orderplatform.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orderplatform.common.enums.RefundStatusEnum;
import com.orderplatform.payment.entity.RefundRecord;
import com.orderplatform.payment.service.RefundService;
import com.orderplatform.payment.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class RefundTimeoutTask {

    @Autowired
    private RefundService refundService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutRefund() {
        Boolean enabled = systemConfigService.getConfigBoolean("refund.timeout.enabled", true);
        if (!enabled) {
            log.info("退款超时自动驳回功能已关闭，跳过处理");
            return;
        }

        Integer timeoutHours = systemConfigService.getConfigInt("refund.timeout.hours", 72);
        LocalDateTime timeoutTime = LocalDateTime.now().minusHours(timeoutHours);

        log.info("开始处理超时退款申请，超时时间：{}小时，超时时间点：{}", timeoutHours, timeoutTime);

        List<RefundRecord> timeoutRefunds = refundService.list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundStatus, RefundStatusEnum.PENDING_AUDIT.getCode())
                .le(RefundRecord::getCreateTime, timeoutTime));

        if (timeoutRefunds.isEmpty()) {
            log.info("没有需要处理的超时退款申请");
            return;
        }

        log.info("发现{}条超时退款申请需要处理", timeoutRefunds.size());

        int successCount = 0;
        for (RefundRecord refund : timeoutRefunds) {
            try {
                refund.setRefundStatus(RefundStatusEnum.AUDIT_REJECT.getCode());
                refund.setAuditRemark("系统自动驳回：退款申请超时未审核");
                refund.setUpdateTime(LocalDateTime.now());
                refundService.updateById(refund);
                successCount++;
                log.info("超时退款申请已自动驳回，退款单号：{}", refund.getRefundNo());
            } catch (Exception e) {
                log.error("驳回超时退款申请失败，退款单号：{}", refund.getRefundNo(), e);
            }
        }

        log.info("超时退款申请处理完成，共处理{}条，成功{}条", timeoutRefunds.size(), successCount);
    }
}
