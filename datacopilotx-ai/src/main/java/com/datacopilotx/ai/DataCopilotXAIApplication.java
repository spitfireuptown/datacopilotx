package com.datacopilotx.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.datacopilotx.ai.mapper")
@SpringBootApplication(scanBasePackages = "com.datacopilotx.*.**")
public class DataCopilotXAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCopilotXAIApplication.class, args);
    }
}
