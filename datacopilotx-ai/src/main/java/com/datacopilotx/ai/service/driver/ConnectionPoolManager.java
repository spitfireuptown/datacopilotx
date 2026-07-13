package com.datacopilotx.ai.service.driver;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.driver.mysql.DefaultMySQLDriver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据库连接池管理器 - 管理不同数据源的连接池
 * @author: AI Assistant
 */
@Slf4j
public class ConnectionPoolManager {

    private static final Map<String, DataSourceEntry> dataSourceMap = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CLEANUP_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "connection-pool-cleanup");
        t.setDaemon(true);
        return t;
    });

    private static final long IDLE_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;

    static {
        CLEANUP_SCHEDULER.scheduleAtFixedRate(ConnectionPoolManager::cleanupIdleDataSources,
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(ConnectionPoolManager::closeAllDataSources));
    }

    public static Connection getConnection(DataSetDTO.DriverInfo driverInfo, JDBCDriver jdbcDriver) throws SQLException {
        String dataSourceKey = generateDataSourceKey(driverInfo);
        DataSourceEntry entry = dataSourceMap.get(dataSourceKey);

        if (entry == null) {
            synchronized (ConnectionPoolManager.class) {
                if (dataSourceMap.get(dataSourceKey) == null) {
                    HikariDataSource dataSource = createDataSource(driverInfo, jdbcDriver);
                    entry = new DataSourceEntry(dataSource);
                    dataSourceMap.put(dataSourceKey, entry);
                    log.info("为数据源 {} 创建了新的连接池", dataSourceKey);
                } else {
                    entry = dataSourceMap.get(dataSourceKey);
                }
            }
        }

        entry.touch();
        return entry.getDataSource().getConnection();
    }

    private static HikariDataSource createDataSource(DataSetDTO.DriverInfo driverInfo, JDBCDriver jdbcDriver) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcDriver.jdbcUrl());
        config.setDriverClassName(jdbcDriver.driverClass());
        
        config.setUsername(driverInfo.getUsername());
        config.setPassword(driverInfo.getPassword());
        
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(10);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setValidationTimeout(5000);
        
        if (Arrays.asList("mysql", "excel").contains(driverInfo.getType())) {
            config.setConnectionTestQuery("SELECT 1");
        } else if ("clickhouse".equals(driverInfo.getType())) {
            config.setConnectionTestQuery("SELECT 1");
        }

        return new HikariDataSource(config);
    }

    private static void cleanupIdleDataSources() {
        long now = System.currentTimeMillis();
        int cleanedCount = 0;
        
        for (Map.Entry<String, DataSourceEntry> entry : dataSourceMap.entrySet()) {
            if (now - entry.getValue().getLastUsedTime() > IDLE_TIMEOUT_MS) {
                String key = entry.getKey();
                DataSourceEntry dsEntry = dataSourceMap.remove(key);
                if (dsEntry != null) {
                    try {
                        dsEntry.getDataSource().close();
                        cleanedCount++;
                        log.info("清理空闲数据源 {} 的连接池（{} 毫秒未使用）", key, now - dsEntry.getLastUsedTime());
                    } catch (Exception e) {
                        log.error("关闭数据源 {} 的连接池失败", key, e);
                    }
                }
            }
        }
        
        if (cleanedCount > 0) {
            log.info("本次清理完成，共清理 {} 个空闲连接池", cleanedCount);
        }
    }

    private static String generateDataSourceKey(DataSetDTO.DriverInfo driverInfo) {
        return String.format("%s_%s_%s_%s_%s",
                driverInfo.getType(),
                driverInfo.getHost(),
                driverInfo.getPort(),
                driverInfo.getDatabase(),
                driverInfo.getUsername());
    }

    public static void closeDataSource(DataSetDTO.DriverInfo driverInfo) {
        String dataSourceKey = generateDataSourceKey(driverInfo);
        DataSourceEntry entry = dataSourceMap.remove(dataSourceKey);
        if (entry != null) {
            try {
                entry.getDataSource().close();
                log.info("关闭数据源 {} 的连接池", dataSourceKey);
            } catch (Exception e) {
                log.error("关闭数据源 {} 的连接池失败", dataSourceKey, e);
            }
        }
    }

    public static void closeAllDataSources() {
        CLEANUP_SCHEDULER.shutdown();
        
        for (Map.Entry<String, DataSourceEntry> entry : dataSourceMap.entrySet()) {
            try {
                entry.getValue().getDataSource().close();
                log.info("关闭数据源 {} 的连接池", entry.getKey());
            } catch (Exception e) {
                log.error("关闭数据源 {} 的连接池失败", entry.getKey(), e);
            }
        }
        dataSourceMap.clear();
    }

    private static class DataSourceEntry {
        private final HikariDataSource dataSource;
        private volatile long lastUsedTime;

        public DataSourceEntry(HikariDataSource dataSource) {
            this.dataSource = dataSource;
            this.lastUsedTime = System.currentTimeMillis();
        }

        public HikariDataSource getDataSource() {
            return dataSource;
        }

        public long getLastUsedTime() {
            return lastUsedTime;
        }

        public void touch() {
            this.lastUsedTime = System.currentTimeMillis();
        }
    }
}