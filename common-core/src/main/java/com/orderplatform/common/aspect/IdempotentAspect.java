package com.orderplatform.common.aspect;

import com.orderplatform.common.annotation.Idempotent;
import com.orderplatform.common.exception.BusinessException;
import com.orderplatform.common.lock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisDistributedLock distributedLock;

    @Around("@annotation(com.orderplatform.common.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String key = generateKey(joinPoint, idempotent);
        String lockKey = "lock:" + key;

        boolean locked = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(idempotent.message());
        }

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                log.warn("重复请求被拦截, key: {}", key);
                throw new BusinessException(idempotent.message());
            }

            Object result = joinPoint.proceed();

            redisTemplate.opsForValue().set(key, "1", idempotent.expireTime(), idempotent.timeUnit());
            log.info("幂等性标记已设置, key: {}, expire: {} {}", key, idempotent.expireTime(), idempotent.timeUnit());

            return result;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        StringBuilder keyBuilder = new StringBuilder(idempotent.prefix());

        if (!"".equals(idempotent.key())) {
            keyBuilder.append(idempotent.key());
        } else {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("userId");
                if (userId != null) {
                    keyBuilder.append("user:").append(userId).append(":");
                }
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            keyBuilder.append(className).append(":").append(methodName).append(":");

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                keyBuilder.append(args[0].hashCode());
            }
        }

        return keyBuilder.toString();
    }
}
