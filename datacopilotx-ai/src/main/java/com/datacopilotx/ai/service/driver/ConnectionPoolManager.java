package com.datacopilotx.ai.service.driver;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接池管理器 - 管理不同数据源的连接池
 * @author: AI Assistant
 */
@Slf4j
public class ConnectionPoolManager {

    // 存储不同数据源的连接池
    private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 获取数据库连接
     * @param driverInfo 数据源信息
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    public static Connection getConnection(DataSetDTO.DriverInfo driverInfo, JDBCDriver jdbcDriver) throws SQLException {
        String dataSourceKey = generateDataSourceKey(driverInfo);
        HikariDataSource dataSource = dataSourceMap.get(dataSourceKey);

        // 如果连接池不存在，则创建新的连接池
        if (dataSource == null) {
            synchronized (ConnectionPoolManager.class) {
                // 双重检查锁定，避免并发创建
                if (dataSourceMap.get(dataSourceKey) == null) {
                    dataSource = createDataSource(driverInfo, jdbcDriver);
                    dataSourceMap.put(dataSourceKey, dataSource);
                    log.info("为数据源 {} 创建了新的连接池", dataSourceKey);
                } else {
                    dataSource = dataSourceMap.get(dataSourceKey);
                }
            }
        }

        return dataSource.getConnection();
    }

    /**
     * 创建数据源连接池
     * @param driverInfo 数据源信息
     * @param jdbcDriver JDBC驱动
     * @return Hikari数据源
     */
    private static HikariDataSource createDataSource(DataSetDTO.DriverInfo driverInfo, JDBCDriver jdbcDriver) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcDriver.jdbcUrl());
        config.setDriverClassName(jdbcDriver.driverClass());
        config.setUsername(driverInfo.getUsername());
        config.setPassword(driverInfo.getPassword());
        
        // 设置连接池参数
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(10);
        config.setMaxLifetime(1800000); // 30分钟
        config.setConnectionTimeout(30000); // 30秒
        config.setValidationTimeout(5000); // 5秒
        
        // 根据数据库类型设置合适的测试查询
        if ("mysql".equals(driverInfo.getType())) {
            config.setConnectionTestQuery("SELECT 1");
        } else if ("clickhouse".equals(driverInfo.getType())) {
            config.setConnectionTestQuery("SELECT 1");
        }

        return new HikariDataSource(config);
    }

    /**
     * 生成数据源唯一标识
     * @param driverInfo 数据源信息
     * @return 数据源唯一标识
     */
    private static String generateDataSourceKey(DataSetDTO.DriverInfo driverInfo) {
        return String.format("%s_%s_%s_%s_%s",
                driverInfo.getType(),
                driverInfo.getHost(),
                driverInfo.getPort(),
                driverInfo.getDatabase(),
                driverInfo.getUsername());
    }

    /**
     * 关闭指定数据源的连接池
     * @param driverInfo 数据源信息
     */
    public static void closeDataSource(DataSetDTO.DriverInfo driverInfo) {
        String dataSourceKey = generateDataSourceKey(driverInfo);
        HikariDataSource dataSource = dataSourceMap.remove(dataSourceKey);
        if (dataSource != null) {
            dataSource.close();
            log.info("关闭数据源 {} 的连接池", dataSourceKey);
        }
    }

    /**
     * 关闭所有连接池
     */
    public static void closeAllDataSources() {
        for (Map.Entry<String, HikariDataSource> entry : dataSourceMap.entrySet()) {
            try {
                entry.getValue().close();
                log.info("关闭数据源 {} 的连接池", entry.getKey());
            } catch (Exception e) {
                log.error("关闭数据源 {} 的连接池失败", entry.getKey(), e);
            }
        }
        dataSourceMap.clear();
    }
}