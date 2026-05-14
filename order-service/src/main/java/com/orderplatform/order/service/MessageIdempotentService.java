package com.orderplatform.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageIdempotentService {

    private final Map<String, Long> processedMessages = new ConcurrentHashMap<>();
    private static final long MESSAGE_EXPIRE_TIME = TimeUnit.HOURS.toMillis(24);

    public boolean isProcessed(String messageId) {
        if (messageId == null) {
            return false;
        }
        Long processTime = processedMessages.get(messageId);
        if (processTime == null) {
            return false;
        }
        if (System.currentTimeMillis() - processTime > MESSAGE_EXPIRE_TIME) {
            processedMessages.remove(messageId);
            return false;
        }
        return true;
    }

    public void markAsProcessed(String messageId) {
        if (messageId != null) {
            processedMessages.put(messageId, System.currentTimeMillis());
        }
    }

    public void cleanExpiredMessages() {
        long currentTime = System.currentTimeMillis();
        processedMessages.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > MESSAGE_EXPIRE_TIME
        );
        log.info("已清理过期消息ID，当前缓存大小: {}", processedMessages.size());
    }
}
