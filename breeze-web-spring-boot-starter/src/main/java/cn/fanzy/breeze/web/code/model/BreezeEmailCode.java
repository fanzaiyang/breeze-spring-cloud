package cn.fanzy.breeze.web.code.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 验证码实体类
 *
 * @author fanzaiyang
 * @since 2022-08-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BreezeEmailCode extends BreezeCode{
    public BreezeEmailCode(String code, int maxRetryCode, long expireTimeInSeconds) {
        super(code, maxRetryCode, expireTimeInSeconds);
    }
}
