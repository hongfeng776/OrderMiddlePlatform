package com.orderplatform.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    public void sendOrderCreated(String phone, String orderNo) {
        String content = String.format("【订单履约中台】您的订单%s已创建成功，请及时完成支付。", orderNo);
        sendSms(phone, content);
    }

    public void sendPaymentSuccess(String phone, String orderNo) {
        String content = String.format("【订单履约中台】您的订单%s支付成功，等待商家发货。", orderNo);
        sendSms(phone, content);
    }

    public void sendOrderShipped(String phone, String orderNo) {
        String content = String.format("【订单履约中台】您的订单%s已发货，请注意查收。", orderNo);
        sendSms(phone, content);
    }

    public void sendOrderCompleted(String phone, String orderNo) {
        String content = String.format("【订单履约中台】您的订单%s已完成，感谢您的购买。", orderNo);
        sendSms(phone, content);
    }

    public void sendOrderCancelled(String phone, String orderNo) {
        String content = String.format("【订单履约中台】您的订单%s已取消。", orderNo);
        sendSms(phone, content);
    }

    private void sendSms(String phone, String content) {
        log.info("发送短信 - 手机号: {}, 内容: {}", phone, content);
    }
}
