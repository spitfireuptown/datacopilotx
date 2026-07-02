package com.datacopilotx.autotest;

import com.datacopilotx.ai.DataCopilotXAIApplication;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.common.result.WebResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

@Slf4j
@Testcontainers
@SpringBootTest(classes = DataCopilotXAIApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TextToSqlAccuracyTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("datacopilotx_autotest")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @Autowired
    private WorkflowGraph workflowGraph;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private List<TestQuestion> testQuestions;
    private String testDbUrl;
    private String testDbUsername;
    private String testDbPassword;

    @BeforeAll
    static void startContainer() {
        mysqlContainer.start();
    }

    @AfterAll
    static void stopContainer() {
        mysqlContainer.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        testDbUrl = mysqlContainer.getJdbcUrl();
        testDbUsername = mysqlContainer.getUsername();
        testDbPassword = mysqlContainer.getPassword();

        initTestData();

        try (InputStream is = getClass().getResourceAsStream("/test_questions.json")) {
            testQuestions = objectMapper.readValue(is, new TypeReference<List<TestQuestion>>() {});
        }
        log.info("Loaded {} test questions", testQuestions.size());
    }

    private void initTestData() throws Exception {
        String sqlFilePath = "/mock_sales_test_data.sql";
        try (InputStream is = getClass().getResourceAsStream(sqlFilePath)) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                    jdbcTemplate.getDataSource().getConnection(),
                    new org.springframework.core.io.InputStreamResource(is)
            );
        }
        log.info("Test data initialized");
    }

    @Test
    @Order(1)
    void testDatabaseConnection() {
        String count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dims_shop", String.class);
        Assertions.assertEquals("5", count);
        log.info("Database connection test passed");
    }

    @Test
    @Order(2)
    void testTextToSqlAccuracy() throws Exception {
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

                boolean isCorrect = compareSqlResults(testQuestion.getGold_sql(), predictedSql);

                if (isCorrect) {
                    correctCount++;
                    difficultyCorrect.merge(testQuestion.getDifficulty(), 1, Integer::sum);
                    log.info("Result: CORRECT");
                } else {
                    wrongCount++;
                    wrongCases.add(String.format("ID:%d, Q:%s, Diff:%s, Gold:%s, Pred:%s",
                            testQuestion.getId(), testQuestion.getQuestion(),
                            testQuestion.getDifficulty(), testQuestion.getGold_sql(), predictedSql));
                    log.info("Result: WRONG");
                }
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

        Map<String, Object> initialData = workflowGraph.createInitialState(sessionId, questionId, 1L, 1L, question);
        initialData = new HashMap<>(initialData);
        initialData.put("sink", new SerializableSink(sink));
        initialData.put("data_set_bean", createMockDataSetBean());
        initialData.put("model_config_bean", createMockModelConfigBean());
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
        DataSetBean bean = new DataSetBean();
        bean.setId(1L);
        bean.setDsName("测试数据集");
        bean.setType("mysql");
        bean.setHost("localhost");
        bean.setPort(3306L);
        bean.setDatabase("datacopilotx_autotest");
        bean.setUsername("test");
        bean.setPassword("test");
        bean.setTable("dims_shop,dims_product,facts_sales");
        bean.setFields("dims_shop(shop_id,shop_name,region,city,manager);dims_product(product_id,product_name,category,subcategory,price,cost);facts_sales(id,order_id,shop_id,product_id,sale_time,quantity,price,discount_amount,total_amount)");
        bean.setRelations("facts_sales.shop_id=dims_shop.shop_id;facts_sales.product_id=dims_product.product_id");
        return bean;
    }

    private ModelConfigBean createMockModelConfigBean() {
        ModelConfigBean bean = new ModelConfigBean();
        bean.setId(1L);
        bean.setModel("gpt-4");
        bean.setPlatform("openai");
        bean.setType("openai");
        bean.setFunctionType("chat");
        bean.setApiKey(System.getenv("OPENAI_API_KEY"));
        bean.setBaseUrl("https://api.openai.com/v1");
        return bean;
    }

    private boolean compareSqlResults(String goldSql, String predictedSql) throws Exception {
        if (predictedSql == null || predictedSql.trim().isEmpty()) {
            return false;
        }

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
            return true;
        }

        if (goldRows.size() != predictedRows.size()) {
            return false;
        }

        Set<String> goldColumns = new HashSet<>(goldColumnsList);
        Set<String> predictedColumns = new HashSet<>(predictedColumnsList);

        if (!goldColumns.equals(predictedColumns)) {
            log.info("Gold columns: {}, Predicted columns: {}", goldColumns, predictedColumns);
            return false;
        }

        Set<String> goldRowStrings = new HashSet<>();
        for (Map<String, Object> row : goldRows) {
            goldRowStrings.add(normalizeRow(row, goldColumns));
        }

        Set<String> predictedRowStrings = new HashSet<>();
        for (Map<String, Object> row : predictedRows) {
            predictedRowStrings.add(normalizeRow(row, goldColumns));
        }

        return goldRowStrings.equals(predictedRowStrings);
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

        log.info("========================================");
        log.info("  Text-to-SQL Accuracy Test Report");
        log.info("========================================");
        log.info("Total Questions: {}", total);
        log.info("Correct: {}", correct);
        log.info("Wrong: {}", wrong);
        log.info("Error: {}", error);
        log.info("Accuracy: {}%", String.format("%.2f", accuracy));
        log.info("----------------------------------------");
        log.info("Difficulty Breakdown:");
        for (String difficulty : Arrays.asList("simple", "medium", "complex")) {
            int totalDiff = difficultyStats.get(difficulty);
            int correctDiff = difficultyCorrect.get(difficulty);
            double accDiff = totalDiff > 0 ? (double) correctDiff / totalDiff * 100 : 0;
            log.info("  {}: {}/{} ({})", difficulty, correctDiff, totalDiff, String.format("%.2f%%", accDiff));
        }
        log.info("----------------------------------------");

        if (!wrongCases.isEmpty()) {
            log.info("Wrong Cases:");
            for (String caseInfo : wrongCases) {
                log.info("  - {}", caseInfo);
            }
        }

        log.info("========================================");
    }
}