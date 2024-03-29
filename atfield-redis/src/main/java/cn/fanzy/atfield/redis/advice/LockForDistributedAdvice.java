package cn.fanzy.atfield.redis.advice;

import cn.fanzy.atfield.core.utils.AopUtil;
import cn.fanzy.atfield.redis.annotation.Lock;
import cn.fanzy.atfield.redis.exception.LockException;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁定
 *
 * @author fanzaiyang
 * @date 2023/12/07
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
@ConditionalOnClass(RedissonClient.class)
public class LockForDistributedAdvice {

    private final RedissonClient redissonClient;

    @Pointcut("@annotation(cn.fanzy.atfield.redis.annotation.Lock)")
    public void cut() {
    }

    @Before("cut()")
    public void before(JoinPoint jp) throws InterruptedException {
        Lock lock = AopUtil.getAnnotation(jp, Lock.class);
        if (lock == null) {
            return;
        }
        RLock rlock = redissonClient.getLock(AopUtil.getLockKey(lock.value()));
        boolean tryLock = rlock.tryLock(lock.tryWaitTime(), lock.leaseTime(), lock.unit());
        if (!tryLock) {
            String message = StrUtil.format("该方法【{}】被另外一个线程占用，请稍后再试！{}", AopUtil.getMethodInfo(jp),
                    rlock.getName());
            log.warn(message);
            throw new LockException("500", message);
        }
    }

    @After("cut()")
    public void after(JoinPoint jp) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Lock lock = method.getAnnotation(Lock.class);
        if (lock == null) {
            return;
        }
        RLock rlock = redissonClient.getLock(AopUtil.getLockKey(lock.value()));
        if (rlock.isLocked() || rlock.isHeldByCurrentThread()) {
            try {
                rlock.unlock();
            } catch (Exception e) {
                rlock.forceUnlock();
            }

        }
    }


}
