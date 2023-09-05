package cn.fanzy.breeze.web.web.json.config;

import cn.fanzy.breeze.web.web.json.properties.BreezeWebJsonProperties;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <pre>
 *     返回前端的JSON NULL处理配置
 *     1. 字符串null转为""
 *     2. 集合null转为[]
 *     3. 布尔null 转为false
 *     4. 数字null转为0
 *     5. 实体null转json对象
 * </pre>
 *
 * @author fanzaiyang
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@EnableConfigurationProperties({BreezeWebJsonProperties.class})
@ConditionalOnProperty(prefix = "breeze.web.json", name = {"enable"}, havingValue = "true", matchIfMissing = true)
public class BreezeJacksonWebConfig implements WebMvcConfigurer {
    private final JacksonProperties jacksonProperties;

    private final WebMvcProperties webMvcProperties;
    private final BreezeWebJsonProperties breezeWebJsonProperties;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new ByteArrayHttpMessageConverter());
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        String format = CharSequenceUtil.blankToDefault(jacksonProperties.getDateFormat(), webMvcProperties.getFormat().getDateTime());
        converter.setObjectMapper(new BreezeJacksonObjectMapper(
                CharSequenceUtil.blankToDefault(format, "yyyy-MM-dd HH:mm:ss"),
                CharSequenceUtil.blankToDefault(webMvcProperties.getFormat().getDate(), "yyyy-MM-dd"),
                CharSequenceUtil.blankToDefault(webMvcProperties.getFormat().getTime(), "HH:mm:ss"),
                breezeWebJsonProperties));
        converters.add(1, converter);
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

    }

    @PostConstruct
    public void checkConfig() {
        log.info("「微风组件」开启 <JSON转换器> 相关的配置。");
    }
}
