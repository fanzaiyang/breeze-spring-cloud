package cn.fanzy.breeze.auth.config;

import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouteFunction;
import cn.fanzy.breeze.auth.function.BreezeDefaultRouteFunction;
import cn.fanzy.breeze.auth.properties.BreezeAuthProperties;
import cn.fanzy.breeze.core.utils.BreezeConstants;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.List;


@Slf4j
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties(BreezeAuthProperties.class)
@ConditionalOnProperty(prefix = "breeze.auth.route", name = {"enable"}, havingValue = "true", matchIfMissing = true)
public class BreezeAuthRouteConfiguration implements WebMvcConfigurer {

    private final BreezeAuthProperties properties;

    @Bean
    @ConditionalOnMissingBean(SaRouteFunction.class)
    public SaParamFunction saRouteFunction() {
        return new BreezeDefaultRouteFunction();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(saRouteFunction()))
                .addPathPatterns(properties.getRoute().getPathPatterns())
                .excludePathPatterns(properties.getRoute().getExcludePathPatterns());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // ??????knife4j ????????????
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // ??????swagger-ui????????????
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // ??????????????????
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * ????????????
     */
    @PostConstruct
    public void checkConfig() {
        List<String> list = properties.getRoute().getExcludePathPatterns();
        list.addAll(BreezeConstants.SWAGGER_LIST);
        properties.getRoute().setExcludePathPatterns(list);
        log.info("???????????????????????? <??????SaToken???????????????> ??????????????????????????????{}", JSONUtil.toJsonStr(list));
    }

}