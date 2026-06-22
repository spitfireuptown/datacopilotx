package com.datacopilotx.ai.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.controller.form.DataSetForm;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.vo.DataSetVO;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.driver.mysql.DefaultMySQLDriver;
import com.datacopilotx.ai.util.ExcelAnalysisUtil;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.DataSetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataSetService {

    @Autowired
    DataSetMapper dataSetMapper;
    @Autowired
    QuestionLogMapper questionLogMapper;
    @Autowired
    private KnowledgeLibMapper knowledgeLibMapper;
    @Autowired
    private DefaultMySQLDriver defaultMySQLDriver;
    @Autowired
    Map<String, List<List<String>>> dataSetCache;


    public List<DataSetVO.ListVO> list() {
        return dataSetMapper.selectList(new QueryWrapper<>()).stream().map(dataSetBean -> {
            DataSetVO.ListVO list = new DataSetVO.ListVO();
            list.setId(dataSetBean.getId());
            if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
                list.setTable(dataSetBean.getTable());
            } else {
                list.setTable(dataSetBean.getDatabase() + "." + dataSetBean.getTable());
            }
            list.setName(dataSetBean.getDsName());
            list.setType(dataSetBean.getType());
            list.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dataSetBean.getCtime()));
            return list;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataSetForm.Create createForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setDsName(createForm.getName());
        dataSetBean.setFields(JSONUtil.toJsonStr(createForm.getFields()));
        dataSetBean.setType(createForm.getType());
        dataSetBean.setDescription(createForm.getDescription());
        dataSetBean.setInjectPrompt(createForm.getPrompt());
        dataSetBean.setTable(createForm.getTable());
        dataSetBean.setDatabase(createForm.getDatabase());
        dataSetBean.setHost(createForm.getHost());
        dataSetBean.setPort(createForm.getPort());
        dataSetBean.setUsername(createForm.getUsername());
        dataSetBean.setPassword(createForm.getPassword());

        if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
            this.syncExcelData(createForm.getName(), createForm.getFields());
            dataSetBean.setDatabase(defaultMySQLDriver.getDatabase());
            dataSetBean.setHost(defaultMySQLDriver.getHost());
            dataSetBean.setPort(defaultMySQLDriver.getPort());
            dataSetBean.setUsername(defaultMySQLDriver.getUsername());
            dataSetBean.setPassword(defaultMySQLDriver.getPassword());
        }

        dataSetMapper.insert(dataSetBean);
        return dataSetBean.getId();
    }


    public Long update(DataSetForm.Create updateForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setDsName(updateForm.getName());
        dataSetBean.setDatabase(updateForm.getDatabase());
        dataSetBean.setHost(updateForm.getHost());
        dataSetBean.setPort(updateForm.getPort());
        dataSetBean.setTable(updateForm.getTable());
        dataSetBean.setUsername(updateForm.getUsername());
        dataSetBean.setPassword(updateForm.getPassword());
        dataSetBean.setFields(JSONUtil.toJsonStr(updateForm.getFields()));
        dataSetBean.setType(updateForm.getType());
        dataSetBean.setInjectPrompt(updateForm.getPrompt());
        dataSetBean.setDescription(updateForm.getDescription());

        if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
            dataSetBean.setDatabase(defaultMySQLDriver.getDatabase());
            dataSetBean.setHost(defaultMySQLDriver.getHost());
            dataSetBean.setPort(defaultMySQLDriver.getPort());
            dataSetBean.setUsername(defaultMySQLDriver.getUsername());
            dataSetBean.setPassword(defaultMySQLDriver.getPassword());
        }

        dataSetMapper.update(dataSetBean, new LambdaQueryWrapper<DataSetBean>().eq(DataSetBean::getId, updateForm.getId()));
        return updateForm.getId();
    }


    public List<DataSetDTO.SchemaInfo> tableSchemaInfo(DataSetForm.Create createForm) {
        List<DataSetDTO.SchemaInfo> result;
        try {
            DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                    .builder()
                    .host(createForm.getHost())
                    .port(createForm.getPort())
                    .database(createForm.getDatabase())
                    .table(createForm.getTable())
                    .username(createForm.getUsername())
                    .password(createForm.getPassword())
                    .type(createForm.getType())
                    .build();
            JDBCDriver driver = DriverFactory.getDriver(driverInfo);
            result = driver.fetchColumn(driverInfo);
        } catch (Exception e) {
            throw new DataCopilotXException(e.getMessage());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void del(long id) {
        DataSetBean dataSetBean = dataSetMapper.selectById(id);
        if (dataSetBean == null) {
            throw new DataCopilotXException("删除数据集不存在");
        }
        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                .builder()
                .host(dataSetBean.getHost())
                .port(dataSetBean.getPort())
                .database(dataSetBean.getDatabase())
                .table(dataSetBean.getTable())
                .username(dataSetBean.getUsername())
                .password(dataSetBean.getPassword())
                .type(dataSetBean.getType())
                .build();
        DriverFactory.removeDriver(driverInfo);
        dataSetMapper.deleteById(id);
        knowledgeLibMapper.delete(new LambdaQueryWrapper<KnowledgeLibBean>().eq(KnowledgeLibBean::getDatasetId, id));
        questionLogMapper.delete(new LambdaQueryWrapper<QuestionLogBean>().eq(QuestionLogBean::getDatasetId, id));
    }

    public DataSetVO.DetailVO detail(long id) {
        DataSetBean dataSetBean = dataSetMapper.selectOne(new LambdaQueryWrapper<DataSetBean>().eq(DataSetBean::getId, id));
        DataSetVO.DetailVO detailVO = new DataSetVO.DetailVO();
        detailVO.setId(dataSetBean.getId());
        detailVO.setName(dataSetBean.getDsName());
        detailVO.setType(dataSetBean.getType());
        detailVO.setHost(dataSetBean.getHost());
        detailVO.setPort(dataSetBean.getPort());
        detailVO.setDatabase(dataSetBean.getDatabase());
        detailVO.setTable(dataSetBean.getTable());
        detailVO.setUsername(dataSetBean.getUsername());
        detailVO.setPassword(dataSetBean.getPassword());
        detailVO.setDescription(dataSetBean.getDescription());
        detailVO.setPrompt(dataSetBean.getInjectPrompt());
        detailVO.setFields(JSONUtil.toList(dataSetBean.getFields(), DataSetDTO.SchemaInfo.class));
        return detailVO;
    }

    public List<DataSetDTO.SchemaInfo> fileUpload(MultipartFile file, String name, String description) {
        DataSetDTO.ExcelDataSetInfo analysis = ExcelAnalysisUtil.analysis(file);
        dataSetCache.put(name, analysis.getContext());
        return analysis.getHeaders().stream().map(row -> DataSetDTO.SchemaInfo.builder()
                .fieldName(row)
                .fieldType("VARCHAR")
                .description("")
                .build()).collect(Collectors.toList());
    }

    
    // 完善 syncExcelData 方法
    private void syncExcelData(String tableName, List<DataSetDTO.SchemaInfo> fields) {
        // 创建表SQL
        String createTableSQL = this.createTableSQL(tableName, fields);
    
        // 插入数据SQL
        String insertDataSQL = this.insertDataSQL(tableName, fields);
        
        // 创建 DriverInfo 对象，使用系统数据库连接
        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo.builder()
                .type("excel")
                .database(defaultMySQLDriver.getDatabase())
                .table(tableName)
                .username(defaultMySQLDriver.getUsername())
                .password(defaultMySQLDriver.getPassword())
                .build();
        
        try {
            // 获取连接并创建 Statement
            Connection connection = defaultMySQLDriver.getConnection(driverInfo);
            Statement statement = connection.createStatement();
            
            try {
                // 执行建表SQL
                log.info("Executing create table SQL: {}", createTableSQL);
                statement.execute(createTableSQL);
                log.info("Table {} created successfully", tableName);
                
                // 执行插入数据SQL（如果有数据）
                if (insertDataSQL != null && !insertDataSQL.isEmpty()) {
                    log.info("Executing insert data SQL");
                    int rowsAffected = statement.executeUpdate(insertDataSQL);
                    log.info("Data inserted successfully, affected rows: {}", rowsAffected);
                }
            } finally {
                // 关闭资源
                defaultMySQLDriver.closeResources(statement);
            }
        } catch (Exception e) {
            log.error("Failed to sync Excel data to table {}", tableName, e);
            throw new RuntimeException("Failed to sync Excel data to database", e);
        }
    }

    private String insertDataSQL(String tableName, List<DataSetDTO.SchemaInfo> fields) {
        List<List<String>> data = dataSetCache.get(tableName);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `").append(tableName).append("` (");
        
        // 从 fields 中添加列名
        for (int i = 0; i < fields.size(); i++) {
            sqlBuilder.append("`").append(fields.get(i).getFieldName()).append("`");
            if (i < fields.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(") VALUES ");
        
        // 处理数据行
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            List<String> rowData = data.get(rowIndex);
            if (rowData == null || rowData.isEmpty()) {
                continue; // 跳过空行
            }
            
            sqlBuilder.append("(");
            for (int colIndex = 0; colIndex < fields.size(); colIndex++) {
                String value = "";
                // 如果数据列足够，获取对应值；否则使用空字符串
                if (colIndex < rowData.size()) {
                    value = rowData.get(colIndex);
                }
                
                // 处理字符串值，添加引号和转义
                if (value == null || value.trim().isEmpty()) {
                    sqlBuilder.append("NULL");
                } else {
                    // 获取字段类型
                    String fieldType = fields.get(colIndex).getFieldType();
                    // 根据字段类型判断是否需要引号
                    if (isNumericType(fieldType) && isNumeric(value)) {
                        sqlBuilder.append(value);
                    } else {
                        // 字符串类型，需要添加引号和转义单引号
                        sqlBuilder.append("'").append(value.replace("'", "\\'"))
                                .append("'");
                    }
                }
                
                if (colIndex < fields.size() - 1) {
                    sqlBuilder.append(", ");
                }
            }
            sqlBuilder.append(")");
            
            // 如果不是最后一行，添加逗号
            if (rowIndex < data.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(";");
        
        log.info("Generated INSERT SQL for table {}", tableName);
        return sqlBuilder.toString();
    }
    
    /**
     * 判断字段类型是否为数值类型
     * @param fieldType 字段类型
     * @return 是否为数值类型
     */
    private boolean isNumericType(String fieldType) {
        if (fieldType == null) {
            return false;
        }
        String type = fieldType.toUpperCase();
        return type.contains("INT") || type.contains("DECIMAL") || type.contains("FLOAT") || 
               type.contains("DOUBLE") || type.contains("NUMERIC") || type.contains("BIGINT") ||
               type.contains("SMALLINT") || type.contains("TINYINT");
    }
    
    // 简单判断字符串是否为数字
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private String createTableSQL(String tableName, List<DataSetDTO.SchemaInfo> fields) {
        List<List<String>> context = (List<List<String>>) dataSetCache.get(tableName);
        
        // 生成建表语句
        StringBuilder createTableSQL = new StringBuilder();
        createTableSQL.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        
        // 添加字段定义
        List<String> columnDefinitions = new ArrayList<>();
        for (DataSetDTO.SchemaInfo field : fields) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("  `").append(field.getFieldName()).append("` ");
            
            // 处理字段类型
            String fieldType = field.getFieldType();
            // 确保字段类型有效
            if (fieldType == null || fieldType.trim().isEmpty()) {
                fieldType = "VARCHAR(255)";
            } else if (!fieldType.contains("(")) {
                // 如果字段类型没有指定长度，为常见类型添加默认长度
                switch (fieldType.toUpperCase()) {
                    case "VARCHAR":
                    case "TEXT":
                        fieldType = "VARCHAR(255)";
                        break;
                    case "INT":
                    case "INTEGER":
                        fieldType = "INT";
                        break;
                    case "BIGINT":
                        fieldType = "BIGINT";
                        break;
                    case "DOUBLE":
                    case "DECIMAL":
                        fieldType = "DECIMAL(18,2)";
                        break;
                    default:
                        // 保留其他类型
                        break;
                }
            }
            
            columnDef.append(fieldType);
            
            // 字段描述
            if (field.getDescription() != null && !field.getDescription().trim().isEmpty()) {
                columnDef.append(" COMMENT '").append(field.getDescription().replace("'", "\\'"))
                        .append("'");
            }
            
            columnDefinitions.add(columnDef.toString());
        }
        
        // 添加列定义到SQL
        createTableSQL.append(String.join(",\n", columnDefinitions));
        
        // 添加主键（如果是第一个字段，可以作为主键）
        createTableSQL.append(",\n  PRIMARY KEY (`")
                .append(fields.get(0).getFieldName())
                .append("`) USING BTREE");
        
        createTableSQL.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
        
        log.info("Generated SQL for table {}: {}", tableName, createTableSQL.toString());
        return createTableSQL.toString();
    }
}
