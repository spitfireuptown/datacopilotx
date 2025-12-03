package com.datacopilotx.ai.service.driver.mysql;

import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 默认MySQL驱动实现 - 用于excel类型数据集和默认数据库连接
 */
@Getter
@Component
public class DefaultMySQLDriver extends JDBCDriver {
    
    @Value("${spring.datasource.url}")
    private String jdbcUrlConfig;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Override
    public String jdbcUrl() {
        return jdbcUrlConfig;
    }

    @Override
    public String driverClass() {
        return driverClassName;
    }


}
