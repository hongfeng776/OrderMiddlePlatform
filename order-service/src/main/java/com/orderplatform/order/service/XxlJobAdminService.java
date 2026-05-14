package com.orderplatform.order.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class XxlJobAdminService {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:default_token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public ReturnT<String> triggerJob(String jobHandler, String executorParam) {
        try {
            String url = adminAddresses + "/jobinfo/trigger";
            Map<String, Object> params = new HashMap<>();
            params.put("executorHandler", jobHandler);
            params.put("executorParam", executorParam);
            params.put("addressType", 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("XXL-JOB-ACCESS-TOKEN", accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                int code = result.getIntValue("code");
                String msg = result.getString("msg");
                if (code == 200) {
                    log.info("任务触发成功: jobHandler={}", jobHandler);
                    return ReturnT.SUCCESS;
                } else {
                    log.error("任务触发失败: jobHandler={}, msg={}", jobHandler, msg);
                    return ReturnT.FAIL;
                }
            }
            log.error("任务触发请求失败: jobHandler={}, status={}", jobHandler, response.getStatusCode());
            return ReturnT.FAIL;
        } catch (Exception e) {
            log.error("调用XXL-Job Admin触发任务异常", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    public ReturnT<String> stopJob(int jobId) {
        try {
            String url = adminAddresses + "/jobinfo/stop";
            Map<String, Object> params = new HashMap<>();
            params.put("id", jobId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("XXL-JOB-ACCESS-TOKEN", accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                int code = result.getIntValue("code");
                if (code == 200) {
                    log.info("任务停止成功: jobId={}", jobId);
                    return ReturnT.SUCCESS;
                }
            }
            log.error("任务停止失败: jobId={}", jobId);
            return ReturnT.FAIL;
        } catch (Exception e) {
            log.error("调用XXL-Job Admin停止任务异常", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    public ReturnT<String> triggerJobManual(String jobHandler, String executorParam) {
        try {
            log.info("手动触发任务: jobHandler={}, param={}", jobHandler, executorParam);
            ReturnT<String> result = triggerJob(jobHandler, executorParam);
            return result;
        } catch (Exception e) {
            log.error("手动触发任务异常", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }
}
