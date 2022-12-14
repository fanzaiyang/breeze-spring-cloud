package cn.fanzy.breeze.web.exception.config;

import cn.fanzy.breeze.web.exception.properties.BreezeWebExceptionProperties;
import cn.fanzy.breeze.web.exception.view.BreezeStaticView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;

import javax.annotation.PostConstruct;

/**
 * @author fanzaiyang
 */
@Slf4j
@Configuration
@AutoConfigureAfter(ErrorMvcAutoConfiguration.class)
@EnableConfigurationProperties({ServerProperties.class, BreezeWebExceptionProperties.class})
@ConditionalOnProperty(prefix = "breeze.web.exception", name = {"replace-basic-error"}, havingValue = "true", matchIfMissing = true)
public class BreezeBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static String SPECIAL_OVERRIDE_BEAN = "basicErrorController";


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition(SPECIAL_OVERRIDE_BEAN)) {
            registry.removeBeanDefinition(SPECIAL_OVERRIDE_BEAN);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

//    @Bean
//    @ConditionalOnMissingBean(BreezeDefaultBasicErrorController.class)
//    public BreezeDefaultBasicErrorController breezeDefaultBasicErrorController(ErrorAttributes errorAttributes,
//                                                                               @NotNull ObjectProvider<ErrorViewResolver> errorViewResolvers) {
//        return new BreezeDefaultBasicErrorController(errorAttributes, this.serverProperties.getError(),
//                errorViewResolvers.orderedStream().collect(Collectors.toList()));
//    }

    @Bean(name = "breezeView")
    public View defaultErrorView() {
        return new BreezeStaticView();
    }


    @PostConstruct
    public void init() {
        log.info("???????????????????????? <??????BasicError> ??????????????????");
    }
}
