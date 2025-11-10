package com.datacopilotx.ai.service.driver;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.driver.clickhouse.ClickhouseDriver;
import com.datacopilotx.ai.service.driver.mysql.MySQLDriver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库驱动工厂类 - 根据数据集类型创建对应的JDBC驱动
 * @author: uptown
 * @date: 2025/8/31 15:31
 */
public class DriverFactory {

    // 存储已创建的驱动实例
    private static final Map<String, JDBCDriver> driverPool = new ConcurrentHashMap<>();

    /**
     * 获取数据库驱动
     * @param driverInfo 数据源信息
     * @return JDBC驱动实例
     * @throws Exception 异常
     */
    public static JDBCDriver getDriver(DataSetDTO.DriverInfo driverInfo) throws Exception {
        // 生成驱动缓存键
        String driverKey = generateDriverKey(driverInfo);
        
        // 从缓存获取驱动实例
        return driverPool.computeIfAbsent(driverKey, key -> {
            try {
                // 根据类型创建对应的数据库驱动
                switch (driverInfo.getType()) {
                    case "mysql":
                        return new MySQLDriver(driverInfo);
                    case "clickhouse":
                        return new ClickhouseDriver(driverInfo);
                    default:
                        throw new Exception("不支持的数据库类型: " + driverInfo.getType());
                }
            } catch (Exception e) {
                throw new RuntimeException("创建数据库驱动失败", e);
            }
        });
    }

    /**
     * 生成驱动缓存键
     * @param driverInfo 数据源信息
     * @return 驱动缓存键
     */
    private static String generateDriverKey(DataSetDTO.DriverInfo driverInfo) {
        return String.format("%s_%s_%s_%s",
                driverInfo.getType(),
                driverInfo.getHost(),
                driverInfo.getPort(),
                driverInfo.getDatabase());
    }

    /**
     * 移除指定数据源的驱动缓存
     * @param driverInfo 数据源信息
     */
    public static void removeDriver(DataSetDTO.DriverInfo driverInfo) {
        String driverKey = generateDriverKey(driverInfo);
        driverPool.remove(driverKey);
        // 同时关闭对应的连接池
        ConnectionPoolManager.closeDataSource(driverInfo);
    }
}
