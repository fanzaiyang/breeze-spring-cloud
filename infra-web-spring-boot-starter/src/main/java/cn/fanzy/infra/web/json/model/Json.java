package cn.fanzy.infra.web.json.model;

import cn.fanzy.infra.tlog.common.context.TLogContext;
import cn.fanzy.infra.web.configuration.JsonModelAutoConfiguration;
import cn.fanzy.infra.web.json.property.JsonProperty;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 全局响应对象
 *
 * @author fanzaiyang
 * @date 2023/11/30
 */
@Data
@Accessors(chain = true)
public class Json<T> {

    private static final JsonProperty PROPERTY = JsonModelAutoConfiguration.getJsonProperty();

    /**
     * 链路id
     */
    private String traceId;
    /**
     * 错误码
     */
    private String code;
    /**
     * 消息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;
    /**
     * 当前时间
     */
    private LocalDateTime now;

    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 异常返回前台的类型
     */
    private ShowType showType = ShowType.SILENT;

    /**
     * 错误堆栈
     */
    private Object errorStacks;

    public Json() {
    }

    public Json(String traceId, String code, String message, T data) {
        this.traceId = traceId;
        this.code = code;
        this.message = message;
        this.data = data;
        this.now = LocalDateTime.now();
    }

    public Json(String traceId, String code, String message, T data, ShowType showType) {
        this.traceId = traceId;
        this.code = code;
        this.message = message;
        this.data = data;
        this.now = LocalDateTime.now();
        this.showType = showType;
    }

    public Json(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public Json(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Json(String code, String message, T data, ShowType showType) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.showType = showType;
    }

    /**
     * 成功
     *
     * @return {@link Json}<{@link T}>
     */
    public static <T> Json<T> ok() {
        return ok(null);
    }

    public static <T> Json<T> ok(String message) {
        return ok(null, message, null);
    }

    /**
     * 成功
     *
     * @param data 数据
     * @return {@link Json}<{@link T}>
     */
    public static <T> Json<T> ok(T data) {
        return ok(null, null, data);
    }

    public static <T> Json<T> ok(String code, String message, T data) {
        return new Json<>(StrUtil.blankToDefault(code, PROPERTY.getModel().getDefaultOkCode()),
                StrUtil.blankToDefault(message, PROPERTY.getModel().getDefaultOkMessage()),
                data);
    }


    public static <T> Json<T> fail() {
        return fail(null);
    }

    public static <T> Json<T> fail(String message) {
        return fail(null, message);
    }

    public static <T> Json<T> fail(String code, String message) {
        return fail(code, message, null);
    }

    public static <T> Json<T> fail(T data) {
        return fail(null, null, data);
    }

    public static <T> Json<T> fail(String code, String message, T data) {
        return new Json<>(StrUtil.blankToDefault(code, PROPERTY.getModel().getDefaultFailCode()),
                StrUtil.blankToDefault(message, PROPERTY.getModel().getDefaultFailMessage()),
                data);
    }

    public String getTraceId() {
        if (StrUtil.isNotBlank(traceId)) {
            return traceId;
        }
        try {
            Class<?> forName = Class.forName("cn.fanzy.infra.tlog.common.context.TLogContext");
            if (forName != null) {
                return TLogContext.getTraceId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    public boolean isSuccess() {
        return StrUtil.equals(getCode(), PROPERTY.getModel().getDefaultOkCode());
    }

    public enum ShowType {
        /**
         * 静默，不显示
         */
        SILENT,
        /**
         * 显示为警告Message
         */
        MESSAGE_WARN,
        /**
         * 显示为错误Message
         */
        MESSAGE_ERROR,
        /**
         * 显示为弹窗警告
         */
        MODAL_WARN,
        /**
         * 显示为弹窗错误
         */
        MODAL_ERROR,
        /**
         * 显示为通知警告NOTIFICATION
         */
        NOTIFICATION_WARN,
        /**
         * 显示为通知错误NOTIFICATION
         */
        NOTIFICATION_ERROR;
    }
}