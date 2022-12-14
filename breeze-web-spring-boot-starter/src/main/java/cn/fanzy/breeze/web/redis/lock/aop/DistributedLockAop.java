package cn.fanzy.breeze.web.redis.lock.aop;

import cn.fanzy.breeze.web.redis.lock.annotation.LockDistributed;
import cn.fanzy.breeze.web.utils.SpringUtils;
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
import java.util.Map;

/**
 * @author fanzaiyang
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
@ConditionalOnClass(RedissonClient.class)
public class DistributedLockAop {

    private final RedissonClient redissonClient;

    @Pointcut("@annotation(cn.fanzy.breeze.web.redis.lock.annotation.LockDistributed)")
    public void cut() {
    }

    @Before("cut()")
    public void before(JoinPoint jp) throws InterruptedException {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        LockDistributed lockDistributed = method.getAnnotation(LockDistributed.class);
        if (lockDistributed == null) {
            return;
        }
        String lockName = "";
        String lockKey = lockDistributed.value();
        if (StrUtil.startWith(lockKey, "@")) {
            lockKey = lockKey.replace(lockKey, "@");
            // @开头说明是请求参数
            Map<String, Object> requestParams = SpringUtils.getRequestParams();
            if (requestParams != null) {
                lockName = requestParams.get(lockKey) == null ? null : requestParams.get(lockKey).toString();
            }
            if (StrUtil.isBlank(lockName)) {
                // 取Header
                lockName = SpringUtils.getRequest().getHeader(lockKey);
            }
        }
        // 如果未找到对应的请求参数，就使用全局锁。
        if (StrUtil.isBlank(lockName)) {
            log.warn("未找到「{}」的参数，使用全局锁{}", lockDistributed.value(), lockKey);
            lockName = lockKey;
        }
        RLock lock = redissonClient.getLock(lockName);
        if (lock.isLocked()) {
            return;
        }
        if (lockDistributed.time() == 0) {
            if (lockDistributed.isTry()) {
                lock.tryLock();
                return;
            }
            lock.lock();
            return;
        }
        if (lockDistributed.isTry()) {
            lock.tryLock(lockDistributed.time(), lockDistributed.unit());
            return;
        }
        lock.lock(lockDistributed.time(), lockDistributed.unit());
    }

    @After("cut()")
    public void after(JoinPoint jp) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        LockDistributed lockDistributed = method.getAnnotation(LockDistributed.class);
        if (lockDistributed == null) {
            return;
        }
        RLock lock = redissonClient.getLock(lockDistributed.value());
        if (lock.isLocked()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                lock.forceUnlock();
            }

        }
    }
}
