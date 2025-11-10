package com.datacopilotx.ai.service.driver.mysql;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import lombok.Getter;

/**
 * @author: uptown
 * @date: 2025/8/31 14:51
 */
@Getter
public class MySQLDriver extends JDBCDriver {
    private static final String MYSQL_DATABASE_JDBC_PATTERN = "jdbc:mysql://%s:%s/%s?useCursorFetch=true&useUnicode=true&zeroDateTimeBehavior=round&characterEncoding=UTF8&useInformationSchema=true&useSSL=false";
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private final DataSetDTO.DriverInfo driverInfo;

    public MySQLDriver(DataSetDTO.DriverInfo driverInfo) {
        this.driverInfo = driverInfo;
    }

    @Override
    public String jdbcUrl() {
        return String.format(
                MYSQL_DATABASE_JDBC_PATTERN,
                this.driverInfo.getHost(),
                this.driverInfo.getPort(),
                this.driverInfo.getDatabase()
        );
    }

    @Override
    public String driverClass() {
        return MYSQL_DRIVER_CLASS;
    }
}
