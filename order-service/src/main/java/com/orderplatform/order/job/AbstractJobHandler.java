package com.orderplatform.order.job;

import com.orderplatform.common.lock.RedisDistributedLock;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.service.ScheduleJobLogService;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public abstract class AbstractJobHandler {

    @Autowired
    protected RedisDistributedLock distributedLock;

    @Autowired
    protected ScheduleJobLogService scheduleJobLogService;

    protected static final int MAX_RETRY_COUNT = 3;
    protected static final long RETRY_DELAY_MS = 5000;

    protected abstract String getJobName();

    protected abstract String getJobHandler();

    protected abstract void doExecute(String param, int shardIndex, int shardTotal) throws Exception;

    @Transactional(rollbackFor = Exception.class)
    public void execute() {
        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();
        String jobParam = XxlJobHelper.getJobParam();
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        String executorAddress = getExecutorAddress();

        String lockKey = "job_lock:" + getJobHandler() + ":" + shardIndex;
        boolean locked = distributedLock.tryLock(lockKey, 5, TimeUnit.MINUTES);
        if (!locked) {
            log.warn("任务正在执行中，跳过本次执行: jobHandler={}, shardIndex={}", getJobHandler(), shardIndex);
            XxlJobHelper.log("任务正在执行中，跳过本次执行");
            saveLog(getJobName(), getJobHandler(), jobParam, 1, "任务正在执行中，跳过本次执行",
                    startTime, 0L, null, shardIndex, shardTotal, executorAddress, 0, 0, null, null);
            return;
        }

        String executeResult = null;
        String errorMessage = null;
        Integer status = 1;
        int retryCount = 0;

        try {
            while (retryCount < MAX_RETRY_COUNT) {
                try {
                    XxlJobHelper.log("开始执行任务: jobName={}, shardIndex={}, shardTotal={}, retryCount={}",
                            getJobName(), shardIndex, shardTotal, retryCount);
                    log.info("开始执行任务: jobName={}, shardIndex={}, shardTotal={}, retryCount={}",
                            getJobName(), shardIndex, shardTotal, retryCount);

                    doExecute(jobParam, shardIndex, shardTotal);

                    executeResult = "任务执行成功";
                    status = 1;
                    XxlJobHelper.log("任务执行成功: jobName={}", getJobName());
                    log.info("任务执行成功: jobName={}", getJobName());
                    break;

                } catch (Exception e) {
                    retryCount++;
                    errorMessage = e.getMessage();

                    if (retryCount < MAX_RETRY_COUNT) {
                        XxlJobHelper.log("任务执行失败，准备重试: jobName={}, retryCount={}, error={}",
                                getJobName(), retryCount, e.getMessage());
                        log.warn("任务执行失败，准备重试: jobName={}, retryCount={}, error={}",
                                getJobName(), retryCount, e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        status = 0;
                        XxlJobHelper.log("任务执行失败，已达最大重试次数: jobName={}, error={}", getJobName(), e.getMessage());
                        log.error("任务执行失败，已达最大重试次数: jobName={}, error={}", getJobName(), e.getMessage());
                        XxlJobHelper.handleFail();
                    }
                }
            }
        } finally {
            distributedLock.unlock(lockKey);
            long duration = System.currentTimeMillis() - startMillis;

            Integer alertStatus = 0;
            String alertMessage = null;
            LocalDateTime alertTime = null;

            if (status == 0) {
                try {
                    alertStatus = 1;
                    alertMessage = "任务执行失败，已达最大重试次数";
                    alertTime = LocalDateTime.now();
                    sendAlert(getJobName(), errorMessage, retryCount);
                } catch (Exception e) {
                    log.error("发送告警通知失败", e);
                    alertStatus = 2;
                    alertMessage = "告警通知发送失败: " + e.getMessage();
                }
            }

            saveLog(getJobName(), getJobHandler(), jobParam, status, executeResult,
                    startTime, duration, errorMessage, shardIndex, shardTotal, executorAddress,
                    retryCount, alertStatus, alertMessage, alertTime);
        }
    }

    protected void sendAlert(String jobName, String errorMessage, int retryCount) {
        try {
            Notification notification = new Notification();
            notification.setUserId(1L);
            notification.setTitle("任务执行失败告警 - " + jobName);
            notification.setContent(String.format("任务【%s】执行失败，已重试%d次。错误信息：%s",
                    jobName, retryCount, errorMessage));
            notification.setType(2);
            notification.setReadStatus(0);
            notification.setCreateTime(LocalDateTime.now());
            scheduleJobLogService.saveNotification(notification);

            log.info("告警通知已发送: jobName={}", jobName);
        } catch (Exception e) {
            log.error("保存告警通知失败", e);
            throw e;
        }
    }

    private String getExecutorAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + System.getProperty("server.port", "8080");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void saveLog(String jobName, String jobHandler, String jobParam, Integer status,
                         String executeResult, LocalDateTime executeTime, Long duration,
                         String errorMessage, Integer shardIndex, Integer shardTotal,
                         String executorAddress, Integer retryCount, Integer alertStatus,
                         String alertMessage, LocalDateTime alertTime) {
        try {
            scheduleJobLogService.saveLogWithDetail(jobName, jobHandler, jobParam, status,
                    executeResult, executeTime, duration, errorMessage, shardIndex, shardTotal,
                    executorAddress, retryCount, alertStatus, alertMessage, alertTime);
        } catch (Exception e) {
            log.error("保存任务日志失败", e);
        }
    }
}
