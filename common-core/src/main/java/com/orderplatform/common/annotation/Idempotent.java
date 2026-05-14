package com.orderplatform.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    String prefix() default "idempotent:";

    String key() default "";

    long expireTime() default 5;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String message() default "请勿重复提交";
}
