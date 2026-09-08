package com.datacopilotx.ai.service.driver.clickhouse;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ClickhouseDriver extends JDBCDriver {

    private static final String CLICKHOUSE_DATABASE_JDBC_PATTERN = "jdbc:clickhouse://%s:%s/%s";
    private static final String CLICKHOUSE_DRIVER_CLASS = "ru.yandex.clickhouse.ClickHouseDriver";

    /** ClickHouse字段描述查询SQL */
    private static final String COLUMN_COMMENT_SQL = "SELECT name, comment FROM system.columns WHERE database = ? AND table = ?";

    /** 字段描述缓存(缓存键: 数据库.表名) */
    private final Map<String, Map<String, String>> columnCommentCache = new HashMap<>();


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

    /**
     * ClickHouse无法通过JDBC元数据REMARKS获取字段描述,
     * 直接查询system.columns的comment字段获取
     */
    @Override
    protected String fetchColumnComment(Connection connection, ResultSet columns, String database, String table) throws SQLException {
        Map<String, String> commentMap = loadColumnComments(connection, database, table);
        return commentMap.getOrDefault(columns.getString("COLUMN_NAME"), "");
    }

    /**
     * 查询system.columns获取表字段描述映射(字段名 -> 描述), 按表缓存避免重复查询
     */
    private Map<String, String> loadColumnComments(Connection connection, String database, String table) throws SQLException {
        String cacheKey = database + "." + table;
        Map<String, String> commentMap = columnCommentCache.get(cacheKey);
        if (commentMap != null) {
            return commentMap;
        }

        commentMap = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(COLUMN_COMMENT_SQL)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    commentMap.put(rs.getString("name"), rs.getString("comment"));
                }
            }
        }
        columnCommentCache.put(cacheKey, commentMap);
        return commentMap;
    }
}
