package cn.fanzy.infra.captcha.creator;

import cn.fanzy.infra.captcha.bean.CaptchaCodeInfo;
import cn.fanzy.infra.captcha.enums.CaptchaType;
import cn.fanzy.infra.captcha.property.CaptchaProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 移动创建者服务impl
 *
 * @author fanzaiyang
 * @date 2023/12/08
 */
@Slf4j
@RequiredArgsConstructor
public abstract class CaptchaMobileCreatorService implements CaptchaCreatorService {
    public abstract CaptchaCodeInfo generateCode(CaptchaProperty captchaProperty);

    @Override
    public CaptchaCodeInfo generate(CaptchaProperty captchaProperty) {
        return generateCode(captchaProperty);
    }

    @Override
    public boolean isSupported(CaptchaType type) {
        return CaptchaType.MOBILE.equals(type);
    }
}
