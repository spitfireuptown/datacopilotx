package com.datacopilotx.ai.service.driver.base;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.service.driver.ConnectionPoolManager;
import com.datacopilotx.common.exception.DataCopilotXException;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * @author: uptown
 * @date: 2025/8/31 15:11
 */
@Slf4j
public abstract class JDBCDriver {

    /**
     * 获取JDBC URL
     * @return JDBC连接URL
     */
    public abstract String jdbcUrl();

    /**
     * 获取JDBC驱动类名
     * @return 驱动类名
     */
    public abstract String driverClass();

    /**
     * 从连接池获取数据库连接
     * @param driverInfo 数据源信息
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    public Connection getConnection(DataSetDTO.DriverInfo driverInfo) throws SQLException {
        return ConnectionPoolManager.getConnection(driverInfo, this);
    }

    /**
     * 执行SQL查询
     * @param driverInfo 数据源信息
     * @param sql SQL语句
     * @return 查询结果
     * @throws Exception 异常
     */
    public QueryDTO execute(DataSetDTO.DriverInfo driverInfo, String sql) throws Exception {
        if (StringUtils.isEmpty(sql)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        QueryDTO queryDTO = new QueryDTO();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = this.getConnection(driverInfo);
            statement = connection.createStatement();
            log.debug("执行SQL语句: {}", sql);
            ResultSet resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<QueryDTO.Column> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                QueryDTO.Column column = new QueryDTO.Column();
                column.setType(metaData.getColumnTypeName(i));
                column.setName(metaData.getColumnName(i));
                columns.add(column);
            }
            queryDTO.setColumns(columns);
            JSONArray array = new JSONArray();
            while (resultSet.next()) {
                JSONObject json = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    json.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                array.add(json);
            }
            queryDTO.setData(array);
        } catch (Exception e) {
            String errorMsg = "SQL执行失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DataCopilotXException(errorMsg);
        } finally {
            closeResources(statement);
            closeResources(connection);
        }
        return queryDTO;
    }

    /**
     * 获取表结构信息
     * @param driverInfo 数据源信息
     * @return 表结构信息列表
     * @throws Exception 异常
     */
    public List<DataSetDTO.SchemaInfo> fetchColumn(DataSetDTO.DriverInfo driverInfo) throws Exception {
        if (driverInfo == null || StringUtils.isEmpty(driverInfo.getTable())) {
            throw new IllegalArgumentException("表名不能为空");
        }

        Connection connection = null;
        ResultSet rs = null;
        ResultSet columns = null;
        List<DataSetDTO.SchemaInfo> result = new ArrayList<>();

        String schema = driverInfo.getDatabase();
        String catalog = driverInfo.getDatabase();
        String table = driverInfo.getTable();

        try {
            connection = this.getConnection(driverInfo);
            // 检查表是否存在
            rs = connection.getMetaData().getTables(catalog, schema, table, null);
            boolean isExists = rs.next();
            
            if (!isExists) {
                return new ArrayList<>();
            }

            // 获取列信息
            columns = connection.getMetaData().getColumns(catalog, schema, table, null);
            while (columns.next()) {
                DataSetDTO.SchemaInfo schemaInfo = new DataSetDTO.SchemaInfo();
                schemaInfo.setFieldName(columns.getString("COLUMN_NAME"));
                schemaInfo.setFieldType(columns.getString("TYPE_NAME"));
                schemaInfo.setDescription(columns.getString("REMARKS"));
                result.add(schemaInfo);
            }
            
            return result;
        } catch (SQLException e) {
            String errorMsg = "获取表结构信息失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DataCopilotXException(errorMsg);
        } finally {
            closeResources(rs, columns);
            closeResources(connection);
        }
    }

    /**
     * 关闭JDBC资源
     * @param resources 要关闭的资源对象
     */
    public void closeResources(AutoCloseable... resources) {
        if (resources != null) {
            for (AutoCloseable resource : resources) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        log.warn("关闭资源失败", e);
                    }
                }
            }
        }
    }
}
