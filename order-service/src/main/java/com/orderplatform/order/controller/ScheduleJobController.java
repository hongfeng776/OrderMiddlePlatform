package com.orderplatform.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderplatform.common.result.Result;
import com.orderplatform.order.entity.ScheduleJobLog;
import com.orderplatform.order.service.JobLogExportService;
import com.orderplatform.order.service.ScheduleJobLogService;
import com.orderplatform.order.service.XxlJobAdminService;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/schedule/job")
public class ScheduleJobController {

    @Autowired
    private ScheduleJobLogService scheduleJobLogService;

    @Autowired
    private XxlJobAdminService xxlJobAdminService;

    @Autowired
    private JobLogExportService jobLogExportService;

    @GetMapping("/logs")
    public Result<Page<ScheduleJobLog>> getJobLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String jobHandler,
            @RequestParam(required = false) Integer status) {
        Page<ScheduleJobLog> result = scheduleJobLogService.listLogs(page, size, jobHandler, status);
        return Result.success(result);
    }

    @GetMapping("/logs/recent")
    public Result<List<ScheduleJobLog>> getRecentLogs(
            @RequestParam(required = false) String jobHandler,
            @RequestParam(defaultValue = "10") int limit) {
        List<ScheduleJobLog> logs = scheduleJobLogService.getRecentLogs(jobHandler, limit);
        return Result.success(logs);
    }

    @GetMapping("/logs/export")
    public void exportLogs(
            @RequestParam(required = false) String jobHandler,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        jobLogExportService.exportLogs(jobHandler, status, response);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getJobStatistics(
            @RequestParam(required = false) String jobHandler) {
        Map<String, Object> statistics = scheduleJobLogService.getJobStatistics(jobHandler);
        return Result.success(statistics);
    }

    @PostMapping("/trigger")
    public Result<String> triggerJob(@RequestBody Map<String, String> params) {
        String jobHandler = params.get("jobHandler");
        String executorParam = params.get("executorParam");

        if (jobHandler == null || jobHandler.isEmpty()) {
            return Result.error("任务处理器不能为空");
        }

        ReturnT<String> result = xxlJobAdminService.triggerJobManual(jobHandler, executorParam);
        if (result.getCode() == ReturnT.SUCCESS_CODE) {
            return Result.success("任务触发成功");
        } else {
            return Result.error("任务触发失败: " + result.getMsg());
        }
    }

    @PostMapping("/stop/{jobId}")
    public Result<String> stopJob(@PathVariable int jobId) {
        ReturnT<String> result = xxlJobAdminService.stopJob(jobId);
        if (result.getCode() == ReturnT.SUCCESS_CODE) {
            return Result.success("任务停止成功");
        } else {
            return Result.error("任务停止失败");
        }
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getJobList() {
        List<Map<String, Object>> jobs = List.of(
                createJobInfo(1, "orderTimeoutCancelJobHandler", "订单超时取消", "每5分钟执行一次",
                        "自动取消超时未支付的订单，支持分片广播", "0 */5 * * * ?", true),
                createJobInfo(2, "orderArchiveJobHandler", "订单数据归档", "每天凌晨2点执行",
                        "归档90天前的已完成、已取消订单到归档表", "0 0 2 * * ?", false),
                createJobInfo(3, "inventoryWarningJobHandler", "库存预警", "每小时执行一次",
                        "检查库存低于阈值的商品并发送预警通知", "0 0 */1 * * ?", false)
        );
        return Result.success(jobs);
    }

    private Map<String, Object> createJobInfo(int jobId, String handler, String name, String schedule,
                                               String description, String cron, boolean sharding) {
        Map<String, Object> job = new HashMap<>();
        job.put("jobId", jobId);
        job.put("handler", handler);
        job.put("name", name);
        job.put("schedule", schedule);
        job.put("description", description);
        job.put("cron", cron);
        job.put("sharding", sharding);
        job.put("maxRetry", 3);
        job.put("retryDelay", "5秒");
        job.put("alertEnabled", true);
        return job;
    }
}
