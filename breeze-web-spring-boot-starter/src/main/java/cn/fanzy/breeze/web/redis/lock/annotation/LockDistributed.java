package cn.fanzy.breeze.web.redis.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;
/**
 * @author fanzaiyang
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockDistributed {
    /**
     * 锁名称
     *
     * @return String
     */
    String value() default "breeze_lock";

    /**
     * 过期时间
     *
     * @return long
     */
    long time() default 0;

    /**
     * 时间单位，默认：秒
     *
     * @return TimeUnit
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 使用tryLock
     * @return boolen
     */
    boolean isTry() default false;
}
