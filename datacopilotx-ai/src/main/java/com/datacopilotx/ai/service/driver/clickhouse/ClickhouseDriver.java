package com.datacopilotx.ai.service.driver.clickhouse;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;

public class ClickhouseDriver extends JDBCDriver {

    private static final String CLICKHOUSE_DATABASE_JDBC_PATTERN = "jdbc:clickhouse://%s:%s/%s";
    private static final String CLICKHOUSE_DRIVER_CLASS = "ru.yandex.clickhouse.ClickHouseDriver";


    private final DataSetDTO.DriverInfo driverInfo;

    public ClickhouseDriver(DataSetDTO.DriverInfo driverInfo) {
        this.driverInfo = driverInfo;
    }

    @Override
    public String jdbcUrl() {
        return String.format(
                CLICKHOUSE_DATABASE_JDBC_PATTERN,
                this.driverInfo.getHost(),
                this.driverInfo.getPort(),
                this.driverInfo.getDatabase()
        );
    }

    @Override
    public String driverClass() {
        return CLICKHOUSE_DRIVER_CLASS;
    }
}
