package cn.fanzy.atfield.captcha.exception;


import cn.fanzy.atfield.core.exception.GlobalException;

import java.io.Serial;

/**
 * 没有captcha例外
 *
 * @author fanzaiyang
 * @date 2023/12/11
 */
public class NoCaptchaException extends GlobalException {
    @Serial
    private static final long serialVersionUID = -291132309394777982L;

    public NoCaptchaException(String code, String message) {
        super(code, message);
    }
}
