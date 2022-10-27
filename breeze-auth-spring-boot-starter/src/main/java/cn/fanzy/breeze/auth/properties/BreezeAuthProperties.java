package cn.fanzy.breeze.auth.properties;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


/**
 * MinIO属性配置
 *
 * @author fanzaiyang
 * @date 2021/09/07
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "breeze.auth")
public class BreezeAuthProperties {

    /**
     * 路由鉴权
     */
    private Route route = new Route();

    /**
     * 注解鉴权
     */
    private Annotations annotation = new Annotations();

    /**
     * jwt token需要引入插件
     * <pre>
     *     &lt;dependency&gt;
     *          &lt;groupId&gt;cn.dev33&lt;/groupId&gt;
     *          &lt;artifactId&gt;sa-token-jwt&lt;/artifactId&gt;
     *          &lt;version&gt;${satoken.version}&lt;/version&gt;
     *     &lt;/dependency&gt;
     * </pre>
     */
    private Jwt jwt = new Jwt();

    private Safe safe = new Safe();


    @Data
    public static class Route {
        /**
         * 是否启用，默认：true
         */
        private Boolean enable;
        /**
         * 拦截路径，默认/**
         */
        private List<String> pathPatterns = CollUtil.newArrayList("/**");
        /**
         * 忽略路径
         */
        private List<String> excludePathPatterns = new ArrayList<>();
    }

    /**
     * 注解鉴权
     *
     * @author fanzaiyang
     * @date 2022-08-23
     */
    @Data
    public static class Annotations {
        /**
         * 是否启用，默认：true
         */
        private Boolean enable;
        /**
         * 拦截路径，默认/**
         */
        private List<String> pathPatterns = CollUtil.newArrayList("/**");
        /**
         * 忽略路径
         */
        private List<String> excludePathPatterns = new ArrayList<>();
    }


    @Data
    public static class Jwt {
        /**
         * 是否启用，默认：false
         */
        private Boolean enable;
        /**
         * Jwt风格
         */
        private JwtMode mode = JwtMode.simple;

        public enum JwtMode {
            /**
             * Simple 模式：Token 风格替换,默认
             */
            simple,
            /**
             * Mixin 模式：混入部分逻辑
             */
            mixin,
            /**
             * Stateless 模式：服务器完全无状态
             */
            stateless;

        }
    }

    /**
     * 安全相关
     */
    @Data
    public static class Safe {
        /**
         * 保存登录次数的key前缀
         */
        private String loginFailedPrefix = "breeze_auth_safe:";

        /**
         * 登录失败有效期，默认24小时。
         */
        private int loginTimeoutSecond = 24 * 60 * 60;
        /**
         * 是否需要验证码，默认：false
         */
        private boolean needCode = false;

        /**
         * 登录失败x次，后需启用验证码
         */
        private int loginFailedShowCodeMaxNum = 3;


    }
}
