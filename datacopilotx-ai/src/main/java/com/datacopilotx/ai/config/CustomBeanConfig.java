package com.datacopilotx.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CustomBeanConfig {

    @Bean
    public Map<String, List<List<String>>> dataSetCache() {
        return new HashMap<>();
    }
}
