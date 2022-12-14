package cn.fanzy.breeze.web.redis.rate.aop;

import cn.fanzy.breeze.web.redis.rate.annotation.RateLimit;
import cn.fanzy.breeze.web.utils.JoinPointUtils;
import cn.fanzy.breeze.web.utils.SpringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * @author fanzaiyang
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
@ConditionalOnClass(RedissonClient.class)
public class RateLimitAop {
    private final RedissonClient redissonClient;

    @Pointcut("@annotation(cn.fanzy.breeze.web.redis.rate.annotation.RateLimit)")
    public void cut() {
    }

    @Before("cut()")
    public void before(JoinPoint jp) {
        RateLimit annotation = JoinPointUtils.getAnnotation(jp, RateLimit.class);
        if (annotation == null) {
            return;
        }
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(getRateKey(annotation));
        if (!rateLimiter.isExists()) {
            rateLimiter.delete();
            rateLimiter.trySetRate(annotation.type(), annotation.rate(), annotation.rateInterval(), annotation.timeUnit());
        }
        rateLimiter.clearExpire();
        boolean acquire = rateLimiter.tryAcquire();
        if (!acquire) {
            throw new RuntimeException(annotation.useIp() ? "请求过于频繁，请稍后在试～" : "当前访问用户太多，请稍后在试～");
        }
    }

    @After("cut()")
    public void after(JoinPoint jp) {
//        RateLimit annotation = JoinPointUtils.getAnnotation(jp, RateLimit.class);
//        if (annotation == null) {
//            return;
//        }
//        RRateLimiter rateLimiter = redissonClient.getRateLimiter(getRateKey(annotation));
//        if(rateLimiter.isExists()){
//            rateLimiter.delete();
//        }
    }


    private String getRateKey(RateLimit annotation) {
        String key = "breeze_rate:";
        String clientIp = SpringUtils.getClientIp();
        if (annotation.useIp()) {
            key += clientIp + ":";
        }
        String requestUri = SpringUtils.getRequestUri().replaceAll("/", "_");
        key += requestUri;
        return key;
    }
}
