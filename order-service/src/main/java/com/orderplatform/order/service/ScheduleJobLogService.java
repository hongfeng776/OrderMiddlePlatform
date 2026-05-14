package com.orderplatform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.order.entity.Notification;
import com.orderplatform.order.entity.ScheduleJobLog;
import com.orderplatform.order.mapper.NotificationMapper;
import com.orderplatform.order.mapper.ScheduleJobLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScheduleJobLogService extends ServiceImpl<ScheduleJobLogMapper, ScheduleJobLog> {

    @Autowired
    private NotificationMapper notificationMapper;

    public void saveLog(String jobName, String jobHandler, String jobParam, Integer status,
                        String executeResult, LocalDateTime executeTime, Long duration,
                        String errorMessage) {
        ScheduleJobLog jobLog = new ScheduleJobLog();
        jobLog.setJobName(jobName);
        jobLog.setJobHandler(jobHandler);
        jobLog.setJobParam(jobParam);
        jobLog.setStatus(status);
        jobLog.setExecuteResult(executeResult);
        jobLog.setExecuteTime(executeTime);
        jobLog.setDuration(duration);
        jobLog.setErrorMessage(errorMessage);
        jobLog.setCreateTime(LocalDateTime.now());
        save(jobLog);
    }

    public void saveLogWithDetail(String jobName, String jobHandler, String jobParam, Integer status,
                                   String executeResult, LocalDateTime executeTime, Long duration,
                                   String errorMessage, Integer shardIndex, Integer shardTotal,
                                   String executorAddress, Integer retryCount, Integer alertStatus,
                                   String alertMessage, LocalDateTime alertTime) {
        ScheduleJobLog jobLog = new ScheduleJobLog();
        jobLog.setJobName(jobName);
        jobLog.setJobHandler(jobHandler);
        jobLog.setJobParam(jobParam);
        jobLog.setStatus(status);
        jobLog.setExecuteResult(executeResult);
        jobLog.setExecuteTime(executeTime);
        jobLog.setDuration(duration);
        jobLog.setErrorMessage(errorMessage);
        jobLog.setShardIndex(shardIndex);
        jobLog.setShardTotal(shardTotal);
        jobLog.setExecutorAddress(executorAddress);
        jobLog.setRetryCount(retryCount);
        jobLog.setAlertStatus(alertStatus);
        jobLog.setAlertMessage(alertMessage);
        jobLog.setAlertTime(alertTime);
        jobLog.setCreateTime(LocalDateTime.now());
        save(jobLog);
    }

    public void saveNotification(Notification notification) {
        notificationMapper.insert(notification);
    }

    public Page<ScheduleJobLog> listLogs(int page, int size, String jobHandler, Integer status) {
        LambdaQueryWrapper<ScheduleJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            wrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        if (status != null) {
            wrapper.eq(ScheduleJobLog::getStatus, status);
        }
        wrapper.orderByDesc(ScheduleJobLog::getExecuteTime);
        return page(new Page<>(page, size), wrapper);
    }

    public List<ScheduleJobLog> getRecentLogs(String jobHandler, int limit) {
        LambdaQueryWrapper<ScheduleJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            wrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        wrapper.orderByDesc(ScheduleJobLog::getExecuteTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    public Map<String, Object> getJobStatistics(String jobHandler) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<ScheduleJobLog> totalWrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            totalWrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        long totalCount = count(totalWrapper);
        result.put("totalCount", totalCount);

        LambdaQueryWrapper<ScheduleJobLog> successWrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            successWrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        successWrapper.eq(ScheduleJobLog::getStatus, 1);
        long successCount = count(successWrapper);
        result.put("successCount", successCount);

        LambdaQueryWrapper<ScheduleJobLog> failWrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            failWrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        failWrapper.eq(ScheduleJobLog::getStatus, 0);
        long failCount = count(failWrapper);
        result.put("failCount", failCount);

        LambdaQueryWrapper<ScheduleJobLog> avgWrapper = new LambdaQueryWrapper<>();
        if (jobHandler != null && !jobHandler.isEmpty()) {
            avgWrapper.eq(ScheduleJobLog::getJobHandler, jobHandler);
        }
        avgWrapper.orderByDesc(ScheduleJobLog::getExecuteTime);
        avgWrapper.last("LIMIT 100");
        List<ScheduleJobLog> recentLogs = list(avgWrapper);
        double avgDuration = recentLogs.stream()
                .filter(log -> log.getDuration() != null)
                .mapToLong(ScheduleJobLog::getDuration)
                .average()
                .orElse(0.0);
        result.put("avgDuration", Math.round(avgDuration));

        return result;
    }
}
