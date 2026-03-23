package com.mindcompass.api.infra.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Flyway 마이그레이션이 끝난 뒤 JPA 검증이 시작되도록 의존 순서를 고정한다.
public class FlywayJpaDependencyConfig {

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlywayInitializer() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!beanFactory.containsBeanDefinition("entityManagerFactory")
                        || !beanFactory.containsBeanDefinition("flywayInitializer")) {
                    return;
                }

                BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
                entityManagerFactory.setDependsOn("flywayInitializer");
            }
        };
    }
}
