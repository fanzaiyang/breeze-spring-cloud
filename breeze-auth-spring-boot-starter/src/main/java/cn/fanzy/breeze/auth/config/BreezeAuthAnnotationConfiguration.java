package cn.fanzy.breeze.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.fanzy.breeze.auth.properties.BreezeAuthProperties;
import cn.fanzy.breeze.core.utils.BreezeConstants;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@ConditionalOnProperty(prefix = "breeze.auth.annotation", name = {"enable"}, havingValue = "true", matchIfMissing = true)
public class BreezeAuthAnnotationConfiguration implements WebMvcConfigurer {

    private final BreezeAuthProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns(properties.getAnnotation().getPathPatterns())
                .excludePathPatterns(properties.getAnnotation().getExcludePathPatterns());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 配置knife4j 显示文档
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 配置swagger-ui显示文档
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 公共部分内容
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/"); }

    /**
     * 配置检查
     */
    @PostConstruct
    public void checkConfig() {
        List<String> list = properties.getAnnotation().getExcludePathPatterns();
        list.addAll(BreezeConstants.SWAGGER_LIST);
        properties.getAnnotation().setExcludePathPatterns(list);
        log.info("「微风组件」开启 <注册SaToken注解拦截器> 相关的配置。白名单：{}", JSONUtil.toJsonStr(list));
    }

}