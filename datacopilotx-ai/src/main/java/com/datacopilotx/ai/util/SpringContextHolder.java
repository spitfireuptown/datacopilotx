package com.datacopilotx.ai.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring上下文持有类，用于在非Spring管理的类中获取Spring容器中的Bean
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }
    
    /**
     * 获取Spring容器中的Bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }
    
    /**
     * 根据名称获取Spring容器中的Bean
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }
}