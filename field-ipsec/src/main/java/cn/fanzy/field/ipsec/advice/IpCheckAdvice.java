package cn.fanzy.field.ipsec.advice;

import cn.fanzy.field.core.spring.SpringUtils;
import cn.fanzy.field.core.utils.AdviceUtil;
import cn.fanzy.field.ipsec.annotation.IpCheck;
import cn.fanzy.field.ipsec.bean.IpStorageBean;
import cn.fanzy.field.ipsec.configuration.IpCheckConfiguration;
import cn.fanzy.field.ipsec.property.IpProperty;
import cn.fanzy.field.ipsec.service.IpCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Aspect
@AutoConfigureAfter(IpCheckConfiguration.class)
@EnableConfigurationProperties({IpProperty.class})
public class IpCheckAdvice {
    private final IpCheckService checkService;

    @Pointcut("@annotation(cn.fanzy.field.ipsec.annotation.IpCheck)")
    public void cut() {
    }

    @Before("cut()")
    public void before(JoinPoint jp) {
        IpCheck annotation = AdviceUtil.getAnnotation(jp, IpCheck.class);
        if (annotation == null) {
            return;
        }
        // 注解优先
        IpStorageBean bean = new IpStorageBean();
        if (annotation.value() != null) {
            bean.setAllowedIpList(Arrays.stream(annotation.value()).collect(Collectors.toSet()));
        }
        if (annotation.deny() != null) {
            bean.setDeniedIpList(Arrays.stream(annotation.deny()).collect(Collectors.toSet()));
        }
        checkService.check(SpringUtils.getClientIp(), bean);
    }
}