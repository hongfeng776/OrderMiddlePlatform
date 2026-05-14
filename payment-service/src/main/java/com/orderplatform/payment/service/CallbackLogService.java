package com.orderplatform.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.common.enums.CallbackStatusEnum;
import com.orderplatform.common.enums.CallbackTypeEnum;
import com.orderplatform.payment.entity.CallbackLog;
import com.orderplatform.payment.mapper.CallbackLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class CallbackLogService extends ServiceImpl<CallbackLogMapper, CallbackLog> {

    public String createCallbackLog(CallbackTypeEnum callbackType, String businessNo, String orderNo, String requestData) {
        String callbackNo = "CALLBACK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        CallbackLog callbackLog = new CallbackLog();
        callbackLog.setCallbackNo(callbackNo);
        callbackLog.setCallbackType(callbackType.getCode());
        callbackLog.setBusinessNo(businessNo);
        callbackLog.setOrderNo(orderNo);
        callbackLog.setRequestData(requestData);
        callbackLog.setCallbackStatus(CallbackStatusEnum.PENDING.getCode());
        callbackLog.setRetryCount(0);
        callbackLog.setCreateTime(LocalDateTime.now());
        callbackLog.setUpdateTime(LocalDateTime.now());
        save(callbackLog);
        
        return callbackNo;
    }

    public void updateCallbackSuccess(String callbackNo, String responseData) {
        CallbackLog callbackLog = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CallbackLog>()
                .eq(CallbackLog::getCallbackNo, callbackNo));
        if (callbackLog != null) {
            callbackLog.setCallbackStatus(CallbackStatusEnum.SUCCESS.getCode());
            callbackLog.setResponseData(responseData);
            callbackLog.setUpdateTime(LocalDateTime.now());
            updateById(callbackLog);
        }
    }

    public void updateCallbackFailed(String callbackNo, String errorMsg) {
        CallbackLog callbackLog = getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CallbackLog>()
                .eq(CallbackLog::getCallbackNo, callbackNo));
        if (callbackLog != null) {
            callbackLog.setCallbackStatus(CallbackStatusEnum.FAILED.getCode());
            callbackLog.setErrorMsg(errorMsg);
            callbackLog.setRetryCount(callbackLog.getRetryCount() + 1);
            callbackLog.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
            callbackLog.setUpdateTime(LocalDateTime.now());
            updateById(callbackLog);
        }
    }
}
