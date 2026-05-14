package com.orderplatform.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisDistributedLock {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    public boolean tryLock(String key, long expireTime, TimeUnit timeUnit) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        lockValueHolder.set(lockValue);
        
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, expireTime, timeUnit);
        
        log.debug("获取分布式锁: key={}, value={}, result={}", lockKey, lockValue, result);
        return Boolean.TRUE.equals(result);
    }

    public boolean tryLock(String key, long waitTime, long expireTime, TimeUnit timeUnit) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        lockValueHolder.set(lockValue);
        
        long waitMillis = timeUnit.toMillis(waitTime);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < waitMillis) {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime, timeUnit);
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("获取分布式锁成功: key={}, value={}", lockKey, lockValue);
                return true;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.warn("获取分布式锁超时: key={}", lockKey);
        return false;
    }

    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockValueHolder.get();
        
        if (lockValue == null) {
            log.warn("释放锁失败: 当前线程未持有锁, key={}", lockKey);
            return;
        }
        
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
            
            if (Long.valueOf(1).equals(result)) {
                log.debug("释放分布式锁成功: key={}, value={}", lockKey, lockValue);
            } else {
                log.warn("释放分布式锁失败: key={}, value={}, 锁可能已过期或被其他线程持有", lockKey, lockValue);
            }
        } finally {
            lockValueHolder.remove();
        }
    }

    public boolean isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}
