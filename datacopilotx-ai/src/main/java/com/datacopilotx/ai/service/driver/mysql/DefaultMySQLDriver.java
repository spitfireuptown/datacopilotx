package com.datacopilotx.ai.service.driver.mysql;

import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 从JDBC URL中解析数据库名
     * @return 数据库名
     */
    public String getDatabase() {
        if (jdbcUrlConfig == null) {
            return null;
        }
        // 匹配 jdbc:mysql://host:port/database 格式
        Pattern pattern = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)");
        Matcher matcher = pattern.matcher(jdbcUrlConfig);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从JDBC URL中解析主机地址
     * @return 主机地址
     */
    public String getHost() {
        if (jdbcUrlConfig == null) {
            return null;
        }
        // 匹配 jdbc:mysql://host:port 格式
        Pattern pattern = Pattern.compile("jdbc:mysql://([^:]+):");
        Matcher matcher = pattern.matcher(jdbcUrlConfig);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从JDBC URL中解析端口
     * @return 端口号
     */
    public Long getPort() {
        if (jdbcUrlConfig == null) {
            return null;
        }
        // 匹配 jdbc:mysql://host:port 格式
        Pattern pattern = Pattern.compile("jdbc:mysql://[^:]+:(\\d+)");
        Matcher matcher = pattern.matcher(jdbcUrlConfig);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
