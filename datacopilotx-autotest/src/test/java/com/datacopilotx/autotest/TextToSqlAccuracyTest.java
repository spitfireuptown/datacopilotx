package com.datacopilotx.autotest;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.DataCopilotXAIApplication;
import com.datacopilotx.ai.controller.form.DataSetForm;
import com.datacopilotx.ai.controller.form.DatasetRelationForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.DatasetRelationMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.service.DataSetService;
import com.datacopilotx.ai.service.DatasetRelationService;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.WorkflowUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

@Slf4j
@SpringBootTest(classes = DataCopilotXAIApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TextToSqlAccuracyTest {

    private static class SqlComparisonResult {
        private final boolean isCorrect;
        private final String reason;

        public SqlComparisonResult(boolean isCorrect, String reason) {
            this.isCorrect = isCorrect;
            this.reason = reason;
        }

        public boolean isCorrect() {
            return isCorrect;
        }

        public String getReason() {
            return reason;
        }
    }

    @Autowired
    private WorkflowGraph workflowGraph;



    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataSetMapper dataSetMapper;

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private DatasetRelationService datasetRelationService;

    @Autowired
    private DatasetRelationMapper datasetRelationMapper;

    @Autowired
    private AIGatewayChatService aiGatewayChatService;

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password}")
    private String dataSourcePassword;

    private List<TestQuestion> testQuestions;
    private String testDbUrl;
    private String testDbUsername;
    private String testDbPassword;
    private Long shopDatasetId;
    private Long productDatasetId;
    private Long salesDatasetId;
    private ModelConfigBean cachedModelConfigBean;

    @BeforeEach
    void setUp() throws Exception {
        testDbUrl = dataSourceUrl;
        testDbUsername = dataSourceUsername;
        testDbPassword = dataSourcePassword;

        initTestData();
        registerTestDataSet();

        cachedModelConfigBean = createMockModelConfigBean();

        try (InputStream is = getClass().getResourceAsStream("/test_questions.json")) {
            testQuestions = objectMapper.readValue(is, new TypeReference<List<TestQuestion>>() {});
        }
        log.info("Loaded {} test questions", testQuestions.size());
    }

    private void initTestData() throws Exception {
        String sqlFilePath = "/mock_sales_test_data.sql";
        try (InputStream is = getClass().getResourceAsStream(sqlFilePath);
             Connection conn = DriverManager.getConnection(testDbUrl, testDbUsername, testDbPassword)) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                    conn,
                    new org.springframework.core.io.InputStreamResource(is)
            );
        }
        log.info("Test data initialized");
    }

    private void registerTestDataSet() throws Exception {
        String dbName = extractDatabaseName(testDbUrl);
        String host = extractHost(testDbUrl);
        Long port = extractPort(testDbUrl);

        List<DataSetDTO.SchemaInfo> shopFields = Arrays.asList(
                new DataSetDTO.SchemaInfo("shop_id", "varchar(20)", "门店唯一编码"),
                new DataSetDTO.SchemaInfo("shop_name", "varchar(100)", "门店全称"),
                new DataSetDTO.SchemaInfo("region", "varchar(50)", "所属大区"),
                new DataSetDTO.SchemaInfo("city", "varchar(50)", "所在城市"),
                new DataSetDTO.SchemaInfo("manager", "varchar(50)", "门店店长姓名")
        );

        List<DataSetDTO.SchemaInfo> productFields = Arrays.asList(
                new DataSetDTO.SchemaInfo("product_id", "varchar(20)", "商品唯一编码"),
                new DataSetDTO.SchemaInfo("product_name", "varchar(200)", "商品名称"),
                new DataSetDTO.SchemaInfo("category", "varchar(50)", "一级品类"),
                new DataSetDTO.SchemaInfo("subcategory", "varchar(50)", "二级品类"),
                new DataSetDTO.SchemaInfo("price", "decimal(10,2)", "标准零售单价"),
                new DataSetDTO.SchemaInfo("cost", "decimal(10,2)", "商品进货成本")
        );

        List<DataSetDTO.SchemaInfo> salesFields = Arrays.asList(
                new DataSetDTO.SchemaInfo("id", "int(11)", "自增主键"),
                new DataSetDTO.SchemaInfo("order_id", "varchar(30)", "订单全局唯一流水号"),
                new DataSetDTO.SchemaInfo("shop_id", "varchar(20)", "门店编码"),
                new DataSetDTO.SchemaInfo("product_id", "varchar(20)", "商品编码"),
                new DataSetDTO.SchemaInfo("sale_time", "datetime", "销售交易时间"),
                new DataSetDTO.SchemaInfo("quantity", "int(11)", "销售数量"),
                new DataSetDTO.SchemaInfo("price", "decimal(10,2)", "单价"),
                new DataSetDTO.SchemaInfo("discount_amount", "decimal(10,2)", "优惠/折扣金额"),
                new DataSetDTO.SchemaInfo("total_amount", "decimal(10,2)", "最终实付总金额")
        );

        DataSetForm.Create shopForm = new DataSetForm.Create();
        shopForm.setName("门店维度表");
        shopForm.setType("mysql");
        shopForm.setHost(host);
        shopForm.setPort(port);
        shopForm.setDatabase(dbName);
        shopForm.setTable("dims_shop");
        shopForm.setUsername(testDbUsername);
        shopForm.setPassword(testDbPassword);
        shopForm.setDescription("门店维度表 - Text-to-SQL测试");
        shopForm.setPrompt("");
        shopForm.setFields(shopFields);
        shopForm.setRelations(new ArrayList<>());
        shopDatasetId = dataSetService.create(shopForm);
        log.info("Shop dataset registered with id: {}", shopDatasetId);

        DataSetForm.Create productForm = new DataSetForm.Create();
        productForm.setName("商品维度表");
        productForm.setType("mysql");
        productForm.setHost(host);
        productForm.setPort(port);
        productForm.setDatabase(dbName);
        productForm.setTable("dims_product");
        productForm.setUsername(testDbUsername);
        productForm.setPassword(testDbPassword);
        productForm.setDescription("商品维度表 - Text-to-SQL测试");
        productForm.setPrompt("");
        productForm.setFields(productFields);
        productForm.setRelations(new ArrayList<>());
        productDatasetId = dataSetService.create(productForm);
        log.info("Product dataset registered with id: {}", productDatasetId);

        DataSetForm.Create salesForm = new DataSetForm.Create();
        salesForm.setName("销售事实表");
        salesForm.setType("mysql");
        salesForm.setHost(host);
        salesForm.setPort(port);
        salesForm.setDatabase(dbName);
        salesForm.setTable("facts_sales");
        salesForm.setUsername(testDbUsername);
        salesForm.setPassword(testDbPassword);
        salesForm.setDescription("销售事实表 - Text-to-SQL测试");
        salesForm.setPrompt("");
        salesForm.setFields(salesFields);
        salesForm.setRelations(new ArrayList<>());
        salesDatasetId = dataSetService.create(salesForm);
        log.info("Sales dataset registered with id: {}", salesDatasetId);

        registerDataSetRelations();
    }

    private void registerDataSetRelations() {
        DatasetRelationForm.Create shopRelation = new DatasetRelationForm.Create();
        shopRelation.setFromDatasetId(salesDatasetId);
        shopRelation.setFromField("shop_id");
        shopRelation.setToDatasetId(shopDatasetId);
        shopRelation.setToField("shop_id");
        shopRelation.setRelationType("JOIN");
        shopRelation.setDescription("销售表与门店表关联");
        datasetRelationService.create(shopRelation);
        log.info("Registered relation: facts_sales.shop_id -> dims_shop.shop_id");

        DatasetRelationForm.Create productRelation = new DatasetRelationForm.Create();
        productRelation.setFromDatasetId(salesDatasetId);
        productRelation.setFromField("product_id");
        productRelation.setToDatasetId(productDatasetId);
        productRelation.setToField("product_id");
        productRelation.setRelationType("JOIN");
        productRelation.setDescription("销售表与商品表关联");
        datasetRelationService.create(productRelation);
        log.info("Registered relation: facts_sales.product_id -> dims_product.product_id");
    }

    private String extractDatabaseName(String jdbcUrl) {
        int start = jdbcUrl.indexOf("/", jdbcUrl.indexOf("//") + 2);
        int end = jdbcUrl.indexOf("?", start);
        if (end == -1) {
            end = jdbcUrl.length();
        }
        return jdbcUrl.substring(start + 1, end);
    }

    private String extractHost(String jdbcUrl) {
        int start = jdbcUrl.indexOf("//") + 2;
        int end = jdbcUrl.indexOf(":", start);
        if (end == -1) {
            end = jdbcUrl.indexOf("/", start);
        }
        return jdbcUrl.substring(start, end);
    }

    private Long extractPort(String jdbcUrl) {
        int start = jdbcUrl.indexOf(":", jdbcUrl.indexOf("//") + 2);
        if (start == -1) {
            return 3306L;
        }
        start++;
        int end = jdbcUrl.indexOf("/", start);
        return Long.parseLong(jdbcUrl.substring(start, end));
    }

    @Test
    @Order(1)
    void testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(testDbUrl, testDbUsername, testDbPassword);
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM dims_shop");
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            String count = rs.getString(1);
            Assertions.assertEquals("5", count);
            log.info("Database connection test passed");
        } catch (Exception e) {
            throw new RuntimeException("Database connection test failed", e);
        }
    }

    @Test
    @Order(2)
    void testTextToSqlAccuracy  () throws Exception {
        int totalQuestions = testQuestions.size();
        int correctCount = 0;
        int wrongCount = 0;
        int errorCount = 0;

        Map<String, Integer> difficultyStats = new HashMap<>();
        difficultyStats.put("simple", 0);
        difficultyStats.put("medium", 0);
        difficultyStats.put("complex", 0);
        Map<String, Integer> difficultyCorrect = new HashMap<>();
        difficultyCorrect.put("simple", 0);
        difficultyCorrect.put("medium", 0);
        difficultyCorrect.put("complex", 0);

        List<String> wrongCases = new ArrayList<>();

        for (TestQuestion testQuestion : testQuestions) {
            log.info("Processing question {}/{}: {}", testQuestion.getId(), totalQuestions, testQuestion.getQuestion());

            difficultyStats.merge(testQuestion.getDifficulty(), 1, Integer::sum);

            try {
                String predictedSql = generateSql(testQuestion.getQuestion());
                log.info("Predicted SQL: {}", predictedSql);
                log.info("Gold SQL: {}", testQuestion.getGold_sql());

                SqlComparisonResult result = compareSqlResults(testQuestion.getQuestion(), testQuestion.getGold_sql(), predictedSql);

                if (result.isCorrect()) {
                    correctCount++;
                    difficultyCorrect.merge(testQuestion.getDifficulty(), 1, Integer::sum);
                    log.info("Result: CORRECT");
                } else {
                    wrongCount++;
                    wrongCases.add(String.format("ID:%d, Q:%s, Diff:%s, Gold:%s, Pred:%s, Reason:%s",
                            testQuestion.getId(), testQuestion.getQuestion(),
                            testQuestion.getDifficulty(), testQuestion.getGold_sql(), predictedSql,
                            result.getReason() != null ? result.getReason() : "No reason provided"));
                    log.info("Result: WRONG - {}", result.getReason());
                }
                Thread.sleep(5000);
            } catch (Exception e) {
                errorCount++;
                log.error("Error processing question {}: {}", testQuestion.getId(), e.getMessage());
                wrongCases.add(String.format("ID:%d, Q:%s, Diff:%s, Error:%s",
                        testQuestion.getId(), testQuestion.getQuestion(),
                        testQuestion.getDifficulty(), e.getMessage()));
            }
        }

        printReport(totalQuestions, correctCount, wrongCount, errorCount, difficultyStats, difficultyCorrect, wrongCases);
    }

    private String generateSql(String question) throws Exception {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = Sinks.many().unicast().onBackpressureBuffer();

        String sessionId = UUID.randomUUID().toString();
        String questionId = UUID.randomUUID().toString();

        DataSetBean dataSetBean = createMockDataSetBean();
        ModelConfigBean modelConfigBean = createMockModelConfigBean();

        Map<String, Object> initialData = workflowGraph.createInitialState(sessionId, questionId, salesDatasetId, modelConfigBean.getId(), question);
        initialData = new HashMap<>(initialData);
        initialData.put("sink", new SerializableSink(sink));
        initialData.put("data_set_bean", dataSetBean);
        initialData.put("model_config_bean", modelConfigBean);
        initialData.put("user_id", "test_user");
        initialData.put("user_role", 0);
        initialData.put("is_admin", true);

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(sessionId)
                .build();

        WorkflowState.initCollectedData();

        StateGraph<WorkflowState> workflow = workflowGraph.createResearchGraph();
        CompiledGraph<WorkflowState> compiledGraph = workflow.compile();

        WorkflowState finalState = null;
        for (var nodeOutput : compiledGraph.stream(initialData, runnableConfig)) {
            finalState = nodeOutput.state();
        }

        finalState.clearCollectedData();

        if (finalState == null) {
            throw new IllegalStateException("Workflow execution did not return any state");
        }

        return finalState.sql().orElse("");
    }

    private DataSetBean createMockDataSetBean() {
        DataSetBean bean = dataSetMapper.selectById(salesDatasetId);
        if (bean == null) {
            throw new IllegalStateException("Sales dataset not found with id: " + salesDatasetId);
        }
        return bean;
    }

    private ModelConfigBean createMockModelConfigBean() {
        ModelConfigBean bean = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigBean>()
                        .eq(ModelConfigBean::getFunctionType, "chat")
                        .orderByAsc(ModelConfigBean::getId)
                        .last("LIMIT 1")
        );
        if (bean == null) {
            throw new IllegalStateException("No chat model found in MODEL_CONFIG table");
        }
        log.info("Using model: {} (id:{}, platform:{})", bean.getModel(), bean.getId(), bean.getPlatform());
        return bean;
    }

    private SqlComparisonResult compareSqlResults(String question, String goldSql, String predictedSql) throws Exception {
        if (predictedSql == null || predictedSql.trim().isEmpty()) {
            return new SqlComparisonResult(false, "Predicted SQL is empty");
        }

        return compareSqlByLLM(question, goldSql, predictedSql);
    }

    private SqlComparisonResult compareSqlByLLM(String question, String goldSql, String predictedSql) throws Exception {
        if (cachedModelConfigBean == null) {
            cachedModelConfigBean = createMockModelConfigBean();
        }

        String systemPrompt = """
                You are an expert SQL semantic analyzer and data validation specialist. Your task is to determine if two SQL queries are functionally equivalent for the business question being asked.
                
                ## Core Principles
                We evaluate SQL correctness based on **business intent** and **result accuracy**, not strict syntactic equivalence. A predicted SQL is considered correct if it returns the data that answers the user's question, even if the implementation differs from the gold SQL.
                
                ## Detailed Rules for Equivalence
                
                ### 1. Field Selection (字段选择)
                - The predicted SQL may return MORE fields than the gold SQL - this is acceptable as long as all fields from the gold SQL are included.
                - The predicted SQL may return fields with different names (aliases) - what matters is that the actual data values are present.
                - If the gold SQL returns a subset of what the predicted SQL returns, they can still be equivalent.
                
                ### 2. Filtering Conditions (过滤条件)
                - Be strict about filter **values** - if the filter value is wrong (e.g., '华东区' vs '华东'), the query may return empty or wrong results.
                - Semantically equivalent field selection is acceptable (e.g., filtering on category vs subcategory if the actual data contains the same records).
                - The key question: do both queries filter to the same logical set of records?
                - Minor variations in filter expressions that yield the same result are acceptable.
                
                ### 3. JOIN and Table Usage (关联查询)
                - The predicted SQL may use JOINs even if the gold SQL uses a single table, as long as the final result contains the correct data.
                - The predicted SQL may use different JOIN types (INNER JOIN, LEFT JOIN) if they produce the same effective result.
                - Using a fact table to JOIN to dimension tables is a valid approach to get dimension data.
                
                ### 4. Deduplication (去重)
                - Using DISTINCT or GROUP BY to eliminate duplicates is acceptable, even if the gold SQL doesn't explicitly use them.
                - If the gold SQL would produce duplicates and the predicted SQL correctly deduplicates, this is considered equivalent.
                
                ### 5. Aggregation Functions (聚合函数)
                - COUNT(*) vs COUNT(DISTINCT primary_key) are equivalent when the primary key is used.
                - The core aggregation operation must be the same (COUNT, SUM, AVG, etc.), but implementation details can vary.
                
                ### 6. Result Set Comparison (结果集比较)
                - For the **core fields** that the question asks for, the predicted SQL's result must **contain all values** from the gold SQL's result.
                - The predicted SQL may return additional rows that are also valid answers to the question.
                - Row order does not matter.
                - IMPORTANT: If the gold SQL returns rows but the predicted SQL returns empty, they are NOT equivalent.
                
                ## Important Considerations
                - Focus on **business correctness**, not technical implementation details.
                - If the predicted SQL answers the user's question correctly, it should be considered equivalent even if it uses a different approach.
                - Consider edge cases like NULL values, empty tables, and data distribution.
                - Always check if the filter values would actually return data in a real database.
                
                ## Output Format
                Return ONLY a JSON object with the following structure:
                {
                    "isEquivalent": true/false,
                    "reason": "Brief explanation of why the SQLs are equivalent or not"
                }
                """;

        String userPrompt = String.format("""
                Business Question: %s
                
                Compare the following two SQL queries for functional equivalence in answering the above business question:
                
                Gold SQL (expected answer):
                %s
                
                Predicted SQL (generated answer):
                %s
                
                Analyze whether the predicted SQL correctly answers the business question. Consider:
                - Does the predicted SQL return the core data requested by the question?
                - Are filtering conditions correct and would they return valid results?
                - Is the aggregation logic correct?
                - Are JOIN operations producing valid results?
                
                Focus on whether the predicted SQL achieves the same business outcome, not whether it matches the gold SQL syntactically.
                """, question, goldSql, predictedSql);

        ChatRequest chatRequest = ChatRequest.builder()
                .model(cachedModelConfigBean.getModel())
                .platform(cachedModelConfigBean.getPlatform())
                .type(cachedModelConfigBean.getType())
                .apiKey(cachedModelConfigBean.getApiKey())
                .baseUrl(cachedModelConfigBean.getBaseUrl())
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .build();

        String response = aiGatewayChatService.chatCompletions(chatRequest);
        log.info("LLM comparison result: {}", response);

        try {
            String cleanResponse = WorkflowUtil.cleanJsonStr(response);
            Map<String, Object> result = cn.hutool.json.JSONUtil.toBean(cleanResponse, Map.class);
            
            Object isEquivalentObj = result.get("isEquivalent");
            boolean isEquivalent;
            if (isEquivalentObj instanceof String) {
                isEquivalent = Boolean.parseBoolean((String) isEquivalentObj);
            } else if (isEquivalentObj instanceof Boolean) {
                isEquivalent = (Boolean) isEquivalentObj;
            } else {
                log.warn("Unexpected isEquivalent type: {}", isEquivalentObj != null ? isEquivalentObj.getClass().getName() : "null");
                return compareSqlByExecution(goldSql, predictedSql);
            }
            
            String reason = (String) result.get("reason");
            log.info("SQL equivalence: {}, Reason: {}", isEquivalent, reason);
            return new SqlComparisonResult(isEquivalent, reason);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response, falling back to result comparison: {}", e.getMessage());
            return compareSqlByExecution(goldSql, predictedSql);
        }
    }

    private SqlComparisonResult compareSqlByExecution(String goldSql, String predictedSql) throws Exception {
        Map<String, Object> goldResults = executeSqlWithColumns(goldSql);
        Map<String, Object> predictedResults = executeSqlWithColumns(predictedSql);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> goldRows = (List<Map<String, Object>>) goldResults.get("rows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> predictedRows = (List<Map<String, Object>>) predictedResults.get("rows");
        @SuppressWarnings("unchecked")
        List<String> goldColumnsList = (List<String>) goldResults.get("columns");
        @SuppressWarnings("unchecked")
        List<String> predictedColumnsList = (List<String>) predictedResults.get("columns");

        log.info("Gold results count: {}, Predicted results count: {}", 
                goldRows.size(), predictedRows.size());

        if (goldRows.isEmpty() && predictedRows.isEmpty()) {
            return new SqlComparisonResult(true, "Both queries return empty result");
        }

        if (goldRows.isEmpty()) {
            if (predictedRows.isEmpty()) {
                return new SqlComparisonResult(true, "Both queries return empty result");
            } else {
                return new SqlComparisonResult(false, "Gold SQL returns empty but predicted SQL returns " + predictedRows.size() + " rows");
            }
        }

        if (predictedRows.isEmpty()) {
            log.warn("Gold SQL returns {} rows but predicted SQL returns empty", goldRows.size());
            return new SqlComparisonResult(false, "Gold SQL returns " + goldRows.size() + " rows but predicted SQL returns empty");
        }

        Set<String> goldColumns = new HashSet<>(goldColumnsList);
        Set<String> predictedColumns = new HashSet<>(predictedColumnsList);

        if (!predictedColumns.containsAll(goldColumns)) {
            log.info("Gold columns: {}, Predicted columns: {}", goldColumns, predictedColumns);
            return new SqlComparisonResult(false, "Predicted SQL missing columns: " + goldColumns);
        }

        Set<String> goldRowStrings = new HashSet<>();
        for (Map<String, Object> row : goldRows) {
            goldRowStrings.add(normalizeRow(row, goldColumns));
        }

        Set<String> predictedRowStrings = new HashSet<>();
        for (Map<String, Object> row : predictedRows) {
            predictedRowStrings.add(normalizeRow(row, goldColumns));
        }

        if (predictedRowStrings.containsAll(goldRowStrings)) {
            return new SqlComparisonResult(true, "Predicted SQL contains all gold SQL results (predicted: " + predictedRows.size() + " rows, gold: " + goldRows.size() + " rows)");
        } else {
            int missingRows = (int) goldRowStrings.stream().filter(r -> !predictedRowStrings.contains(r)).count();
            return new SqlComparisonResult(false, "Predicted SQL missing " + missingRows + " rows from gold SQL results");
        }
    }

    private String normalizeRow(Map<String, Object> row, Set<String> columns) {
        List<String> sortedKeys = new ArrayList<>(columns);
        sortedKeys.sort(String::compareTo);
        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            Object value = row.get(key);
            sb.append(key).append("=").append(normalizeValue(value)).append(";");
        }
        return sb.toString();
    }

    private Object normalizeValue(Object value) {
        if (value instanceof java.math.BigDecimal) {
            java.math.BigDecimal bd = (java.math.BigDecimal) value;
            return bd.stripTrailingZeros().toPlainString();
        }
        return value;
    }

    private Map<String, Object> executeSqlWithColumns(String sql) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(testDbUrl, testDbUsername, testDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value instanceof java.sql.Timestamp) {
                        value = ((java.sql.Timestamp) value).toLocalDateTime();
                    } else if (value instanceof java.sql.Date) {
                        value = ((java.sql.Date) value).toLocalDate();
                    } else if (value instanceof java.math.BigDecimal) {
                        java.math.BigDecimal bd = (java.math.BigDecimal) value;
                        value = bd.stripTrailingZeros().toPlainString();
                    }
                    row.put(columns.get(i - 1), value);
                }
                results.add(row);
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("columns", columns);
        resultMap.put("rows", results);
        return resultMap;
    }

    private void printReport(int total, int correct, int wrong, int error,
                             Map<String, Integer> difficultyStats,
                             Map<String, Integer> difficultyCorrect,
                             List<String> wrongCases) {
        double accuracy = total > 0 ? (double) correct / total * 100 : 0;

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("========================================\n");
        reportBuilder.append("  Text-to-SQL Accuracy Test Report\n");
        reportBuilder.append("========================================\n");
        reportBuilder.append("Test Date: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        reportBuilder.append("Total Questions: ").append(total).append("\n");
        reportBuilder.append("Correct: ").append(correct).append("\n");
        reportBuilder.append("Wrong: ").append(wrong).append("\n");
        reportBuilder.append("Error: ").append(error).append("\n");
        reportBuilder.append("Accuracy: ").append(String.format("%.2f", accuracy)).append("%\n");
        reportBuilder.append("----------------------------------------\n");
        reportBuilder.append("Difficulty Breakdown:\n");
        for (String difficulty : Arrays.asList("simple", "medium", "complex")) {
            int totalDiff = difficultyStats.get(difficulty);
            int correctDiff = difficultyCorrect.get(difficulty);
            double accDiff = totalDiff > 0 ? (double) correctDiff / totalDiff * 100 : 0;
            reportBuilder.append("  ").append(difficulty).append(": ").append(correctDiff).append("/").append(totalDiff)
                    .append(" (").append(String.format("%.2f%%", accDiff)).append(")\n");
        }
        reportBuilder.append("----------------------------------------\n");

        if (!wrongCases.isEmpty()) {
            reportBuilder.append("Wrong Cases:\n");
            for (String caseInfo : wrongCases) {
                reportBuilder.append("  - ").append(caseInfo).append("\n");
            }
        }

        reportBuilder.append("========================================\n");

        String report = reportBuilder.toString();

        log.info("\n{}", report);

        saveReportToFile(report);
    }

    private void saveReportToFile(String report) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "text_to_sql_report_" + timestamp + ".txt";
        String reportsDir = "reports";

        try {
            java.io.File dir = new java.io.File(reportsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            java.io.File reportFile = new java.io.File(dir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(reportFile);
            writer.write(report);
            writer.close();

            log.info("Report saved to: {}", reportFile.getAbsolutePath());
        } catch (java.io.IOException e) {
            log.error("Failed to save report to file: {}", e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        deleteDatasetIfExists(salesDatasetId);
        deleteDatasetIfExists(productDatasetId);
        deleteDatasetIfExists(shopDatasetId);
    }

    private void deleteDatasetIfExists(Long datasetId) {
        if (datasetId != null) {
            try {
                datasetRelationMapper.deleteByDatasetId(datasetId);
                log.info("Relations deleted for dataset: {}", datasetId);
                dataSetService.del(datasetId);
                log.info("Test dataset deleted: {}", datasetId);
            } catch (Exception e) {
                log.warn("Failed to delete test dataset {}: {}", datasetId, e.getMessage());
            }
        }
    }
}