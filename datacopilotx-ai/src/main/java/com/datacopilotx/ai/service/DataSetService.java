package com.datacopilotx.ai.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.controller.form.DataSetForm;
import com.datacopilotx.ai.domian.bean.DataTableBean;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.vo.DataSetVO;
import com.datacopilotx.ai.domian.vo.UserInfoVo;
import com.datacopilotx.ai.mapper.DataTableMapper;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.driver.mysql.DefaultMySQLDriver;
import com.datacopilotx.ai.util.ExcelAnalysisUtil;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.DataSetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.datacopilotx.ai.util.SecurityUtil;

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
    DataTableMapper dataTableMapper;
    @Autowired
    QuestionLogMapper questionLogMapper;
    @Autowired
    private KnowledgeLibMapper knowledgeLibMapper;
    @Autowired
    private DefaultMySQLDriver defaultMySQLDriver;
    @Autowired
    Map<String, List<List<String>>> dataSetCache;
    @Autowired
    AuthService authService;
    @Autowired
    AIGatewayChatService aiGatewayChatService;
    @Autowired
    ModelConfigMapper modelConfigMapper;


    public List<DataSetVO.ListVO> list() {
        return dataSetMapper.selectList(new QueryWrapper<>()).stream().map(dataSetBean -> {
            DataSetVO.ListVO list = new DataSetVO.ListVO();
            list.setId(dataSetBean.getId());
            
            List<DataTableBean> tables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                    .eq(DataTableBean::getDatasetId, dataSetBean.getId())
                    .eq(DataTableBean::getIsDel, 0));
            if (!tables.isEmpty()) {
                if (tables.size() == 1) {
                    list.setTable(tables.get(0).getTable());
                } else {
                    list.setTable(tables.get(0).getTable() + " 等" + tables.size() + "张表");
                }
            } else {
                list.setTable(dataSetBean.getDatabase());
            }
            
            list.setName(dataSetBean.getDsName());
            list.setType(dataSetBean.getType());
            list.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dataSetBean.getCtime()));
            list.setCreator(dataSetBean.getCreator());
            if (dataSetBean.getCreator() != null) {
                try {
                    UserInfoVo user = authService.getUserById(dataSetBean.getCreator());
                    list.setCreatorName(user != null ? user.getNickname() : "");
                } catch (Exception e) {
                    list.setCreatorName("");
                }
            }
            return list;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataSetForm.Create createForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setDsName(createForm.getName());
        dataSetBean.setType(createForm.getType());
        dataSetBean.setDescription(createForm.getDescription());
        dataSetBean.setDatabase(createForm.getDatabase());
        dataSetBean.setHost(createForm.getHost());
        dataSetBean.setPort(createForm.getPort());
        dataSetBean.setUsername(createForm.getUsername());
        dataSetBean.setPassword(createForm.getPassword());
        dataSetBean.setCreator(SecurityUtil.getCurrentUserId());

        if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
            dataSetBean.setDatabase(defaultMySQLDriver.getDatabase());
            dataSetBean.setHost(defaultMySQLDriver.getHost());
            dataSetBean.setPort(defaultMySQLDriver.getPort());
            dataSetBean.setUsername(defaultMySQLDriver.getUsername());
            dataSetBean.setPassword(defaultMySQLDriver.getPassword());
        }

        dataSetMapper.insert(dataSetBean);

        if (createForm.getTables() != null && !createForm.getTables().isEmpty()) {
            for (DataSetForm.Create.TableInfo tableInfo : createForm.getTables()) {
                DataTableBean dataTableBean = new DataTableBean();
                dataTableBean.setDatasetId(dataSetBean.getId());
                dataTableBean.setTable(tableInfo.getTable());
                dataTableBean.setInjectPrompt(tableInfo.getPrompt());
                dataTableBean.setFields(JSONUtil.toJsonStr(tableInfo.getFields()));
                dataTableMapper.insert(dataTableBean);
            }
        }

        batchCalculateTableEmbeddings(dataSetBean.getId());

        return dataSetBean.getId();
    }


    @Transactional(rollbackFor = Exception.class)
    public Long update(DataSetForm.Create updateForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setId(updateForm.getId());
        dataSetBean.setDsName(updateForm.getName());
        dataSetBean.setDatabase(updateForm.getDatabase());
        dataSetBean.setHost(updateForm.getHost());
        dataSetBean.setPort(updateForm.getPort());
        dataSetBean.setUsername(updateForm.getUsername());
        dataSetBean.setPassword(updateForm.getPassword());
        dataSetBean.setType(updateForm.getType());
        dataSetBean.setDescription(updateForm.getDescription());

        if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
            dataSetBean.setDatabase(defaultMySQLDriver.getDatabase());
            dataSetBean.setHost(defaultMySQLDriver.getHost());
            dataSetBean.setPort(defaultMySQLDriver.getPort());
            dataSetBean.setUsername(defaultMySQLDriver.getUsername());
            dataSetBean.setPassword(defaultMySQLDriver.getPassword());
        }

        dataSetMapper.updateById(dataSetBean);

        dataTableMapper.delete(new LambdaQueryWrapper<DataTableBean>().eq(DataTableBean::getDatasetId, updateForm.getId()));

        if (updateForm.getTables() != null && !updateForm.getTables().isEmpty()) {
            for (DataSetForm.Create.TableInfo tableInfo : updateForm.getTables()) {
                DataTableBean dataTableBean = new DataTableBean();
                dataTableBean.setDatasetId(updateForm.getId());
                dataTableBean.setTable(tableInfo.getTable());
                dataTableBean.setInjectPrompt(tableInfo.getPrompt());
                dataTableBean.setFields(JSONUtil.toJsonStr(tableInfo.getFields()));
                dataTableMapper.insert(dataTableBean);
            }
        }

        batchCalculateTableEmbeddings(updateForm.getId());

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

    public List<String> getTables(DataSetForm.Create createForm) {
        List<String> result;
        try {
            DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                    .builder()
                    .host(createForm.getHost())
                    .port(createForm.getPort())
                    .database(createForm.getDatabase())
                    .username(createForm.getUsername())
                    .password(createForm.getPassword())
                    .type(createForm.getType())
                    .build();
            JDBCDriver driver = DriverFactory.getDriver(driverInfo);
            result = driver.fetchTables(driverInfo);
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
                .username(dataSetBean.getUsername())
                .password(dataSetBean.getPassword())
                .type(dataSetBean.getType())
                .build();
        DriverFactory.removeDriver(driverInfo);
        dataTableMapper.delete(new LambdaQueryWrapper<DataTableBean>().eq(DataTableBean::getDatasetId, id));
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
        detailVO.setUsername(dataSetBean.getUsername());
        detailVO.setPassword(dataSetBean.getPassword());
        detailVO.setDescription(dataSetBean.getDescription());
        
        List<DataTableBean> tables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, id)
                .eq(DataTableBean::getIsDel, 0));
        detailVO.setTables(tables.stream().map(table -> {
            DataSetVO.TableVO tableVO = new DataSetVO.TableVO();
            tableVO.setId(table.getId());
            tableVO.setTable(table.getTable());
            tableVO.setPrompt(table.getInjectPrompt());
            tableVO.setFields(JSONUtil.toList(table.getFields(), DataSetDTO.SchemaInfo.class));
            return tableVO;
        }).collect(Collectors.toList()));
        
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


    private void syncExcelData(String tableName, List<DataSetDTO.SchemaInfo> fields) {
        String createTableSQL = this.createTableSQL(tableName, fields);
        String insertDataSQL = this.insertDataSQL(tableName, fields);
        
        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo.builder()
                .type("excel")
                .database(defaultMySQLDriver.getDatabase())
                .table(tableName)
                .username(defaultMySQLDriver.getUsername())
                .password(defaultMySQLDriver.getPassword())
                .build();
        
        try {
            Connection connection = defaultMySQLDriver.getConnection(driverInfo);
            Statement statement = connection.createStatement();
            
            try {
                log.info("Executing create table SQL: {}", createTableSQL);
                statement.execute(createTableSQL);
                log.info("Table {} created successfully", tableName);
                
                if (insertDataSQL != null && !insertDataSQL.isEmpty()) {
                    log.info("Executing insert data SQL");
                    int rowsAffected = statement.executeUpdate(insertDataSQL);
                    log.info("Data inserted successfully, affected rows: {}", rowsAffected);
                }
            } finally {
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
        
        for (int i = 0; i < fields.size(); i++) {
            sqlBuilder.append("`").append(fields.get(i).getFieldName()).append("`");
            if (i < fields.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(") VALUES ");
        
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            List<String> rowData = data.get(rowIndex);
            if (rowData == null || rowData.isEmpty()) {
                continue;
            }
            
            sqlBuilder.append("(");
            for (int colIndex = 0; colIndex < fields.size(); colIndex++) {
                String value = "";
                if (colIndex < rowData.size()) {
                    value = rowData.get(colIndex);
                }
                
                if (value == null || value.trim().isEmpty()) {
                    sqlBuilder.append("NULL");
                } else {
                    String fieldType = fields.get(colIndex).getFieldType();
                    if (isNumericType(fieldType) && isNumeric(value)) {
                        sqlBuilder.append(value);
                    } else {
                        sqlBuilder.append("'").append(value.replace("'", "\\'"))
                                .append("'");
                    }
                }
                
                if (colIndex < fields.size() - 1) {
                    sqlBuilder.append(", ");
                }
            }
            sqlBuilder.append(")");
            
            if (rowIndex < data.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(";");
        
        log.info("Generated INSERT SQL for table {}", tableName);
        return sqlBuilder.toString();
    }
    
    private boolean isNumericType(String fieldType) {
        if (fieldType == null) {
            return false;
        }
        String type = fieldType.toUpperCase();
        return type.contains("INT") || type.contains("DECIMAL") || type.contains("FLOAT") || 
               type.contains("DOUBLE") || type.contains("NUMERIC") || type.contains("BIGINT") ||
               type.contains("SMALLINT") || type.contains("TINYINT");
    }
    
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
        
        StringBuilder createTableSQL = new StringBuilder();
        createTableSQL.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        for (DataSetDTO.SchemaInfo field : fields) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("  `").append(field.getFieldName()).append("` ");
            
            String fieldType = field.getFieldType();
            if (fieldType == null || fieldType.trim().isEmpty()) {
                fieldType = "VARCHAR(255)";
            } else if (!fieldType.contains("(")) {
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
                        break;
                }
            }
            
            columnDef.append(fieldType);
            
            if (field.getDescription() != null && !field.getDescription().trim().isEmpty()) {
                columnDef.append(" COMMENT '").append(field.getDescription().replace("'", "\\'"))
                        .append("'");
            }
            
            columnDefinitions.add(columnDef.toString());
        }
        
        createTableSQL.append(String.join(",\n", columnDefinitions));
        
        createTableSQL.append(",\n  PRIMARY KEY (`")
                .append(fields.get(0).getFieldName())
                .append("`) USING BTREE");
        
        createTableSQL.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
        
        log.info("Generated SQL for table {}: {}", tableName, createTableSQL.toString());
        return createTableSQL.toString();
    }

    private String buildTableSchemaText(String tableName, String injectPrompt, List<DataSetDTO.SchemaInfo> fields) {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("# Table: ").append(tableName);
        if (injectPrompt != null && !injectPrompt.isEmpty()) {
            schemaBuilder.append(", ").append(injectPrompt);
        }
        schemaBuilder.append("\n[\n");

        List<String> fieldList = new ArrayList<>();
        for (DataSetDTO.SchemaInfo field : fields) {
            String fieldComment = field.getDescription() != null ? field.getDescription().strip() : "";
            if (fieldComment.isEmpty()) {
                fieldList.add("(" + field.getFieldName() + ":" + field.getFieldType() + ")");
            } else {
                fieldList.add("(" + field.getFieldName() + ":" + field.getFieldType() + ", " + fieldComment + ")");
            }
        }

        schemaBuilder.append(String.join(",\n", fieldList));
        schemaBuilder.append("\n]\n");

        return schemaBuilder.toString();
    }

    private String calculateTableEmbedding(String tableSchemaText) {
        ModelConfigBean embeddingModel = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigBean>()
                        .eq(ModelConfigBean::getFunctionType, "embedding")
                        .eq(ModelConfigBean::getIsDel, 0)
                        .orderByDesc(ModelConfigBean::getId)
                        .last("LIMIT 1")
        );

        if (embeddingModel == null) {
            log.warn("No embedding model configured, skipping table embedding calculation");
            return null;
        }

        try {
            List<Float> embedding = aiGatewayChatService.embedding(
                    ChatRequest.builder()
                            .apiKey(embeddingModel.getApiKey())
                            .baseUrl(embeddingModel.getBaseUrl())
                            .model(embeddingModel.getModel())
                            .type(embeddingModel.getType())
                            .question(tableSchemaText)
                            .dimensions(embeddingModel.getDimension())
                            .build()
            );

            if (embedding == null || embedding.isEmpty()) {
                log.warn("Failed to generate embedding for table schema");
                return null;
            }

            return JSONUtil.toJsonStr(embedding);
        } catch (Exception e) {
            log.error("Error calculating table embedding: {}", e.getMessage(), e);
            return null;
        }
    }

    public void batchCalculateTableEmbeddings(Long datasetId) {
        List<DataTableBean> tables = dataTableMapper.selectList(
                new LambdaQueryWrapper<DataTableBean>()
                        .eq(DataTableBean::getDatasetId, datasetId)
                        .eq(DataTableBean::getIsDel, 0)
        );

        for (DataTableBean table : tables) {
            List<DataSetDTO.SchemaInfo> fields = JSONUtil.toList(table.getFields(), DataSetDTO.SchemaInfo.class);
            String schemaText = buildTableSchemaText(table.getTable(), table.getInjectPrompt(), fields);
            String embedding = calculateTableEmbedding(schemaText);

            if (embedding != null) {
                table.setEmbedding(embedding);
                dataTableMapper.updateById(table);
            }
        }
    }
}