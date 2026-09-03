package com.datacopilotx.common.constant;

import cn.hutool.core.lang.Pair;

// key: system prompt
// value: user prompt
public interface PromptConstant {

    // 美化提问
    Pair<String, String> BEAUTIFUL_PROMPT = new Pair<>(
            "你是一个自然语言分析高手，擅长结合历史对话理解用户意图，美化用户的提问问题",
            """
            ## 历史对话
            ${history}

            ## 任务
            - 你是一个自然语言分析高手，结合历史对话理解用户当前提问'${query}'的完整意图。
            - 根据问题语义保证在不改变语义的基础上，优化倒装、错别字、口语化等问题。
            - 如果当前提问涉及历史对话中的上下文，需要将省略的指代信息补充完整，形成一个独立、完整的问题。
            - 问题美化为一句话
            ## 输出
            - 只输出美化后的问题，不能输出任何多余内容！！！
            - 不要输出思考过程！！！
            - 不要长篇大论，只输出美化后的问题，一句话。
            """
    );

    // 意图识别与DSL生成
    Pair<String, String> INTENT_RECOGNITION_PROMPT = new Pair<>(
            """
            你是一个自然语言分析高手，判断用户问题"${query}"的意图，擅长分析单表查询和多表联表查询场景。
            """,

            """
            ## 任务
            分析用户问题中的意图，判断用户的问题:"${query}"是否想要查询数据集:"${meta}"中的数据。
            如果是，则解析出完整的查询DSL结构。
            
            **重要：如果问题涉及多个表的字段（如订单表+明细表、用户表+订单表），你必须使用joins进行联表查询！**
            
            **重要：如果问题仅涉及单个维度表的信息（如查询门店名称、商品名称等），不要生成joins字段！**

            ## 数据集Schema
            ${meta}

            ## 查询类型判断规则
            - **单表查询**：当问题仅涉及单个维度表的信息时（如查询门店名称、商品名称、商品价格等），不要生成joins字段，只在tables中包含该维度表
            - **联表查询**：当问题涉及多个表的字段时，必须使用joins字段指定联表关系

            ## 联表查询规则
            - 当用户问题涉及多个表的字段时，必须使用joins字段指定联表关系
            - 仔细阅读"联表关系"和"关联表字段"部分，从中找到正确的关联字段
            - join_type 可选值：INNER JOIN, LEFT JOIN, RIGHT JOIN
            - 如果不确定使用哪种JOIN，默认使用 INNER JOIN
            - 联表时，tables字段必须包含所有涉及的表名
            - fields字段应包含所有需要的字段，用"表名.字段名"格式标注（如"订单表.订单金额"）
            - 过滤条件conditions中的field也使用"表名.字段名"格式

            ## DSL结构说明
            - operation: 操作类型，目前仅支持SELECT
            - tables: 涉及的表名列表
            - fields: 需要查询的字段列表
            - conditions: 查询条件列表
                - field: 字段名
                - operator: 比较运算符 (=, >, <, >=, <=, LIKE, IN等)
                - value: 条件值
                - logical: 逻辑运算符 (AND, OR)，默认AND
            - joins: 关联查询信息（多表关联时需要，单表查询时不要生成此字段）
                - join_type: 连接类型 (INNER JOIN, LEFT JOIN, RIGHT JOIN等)
                - from_table: 左表名
                - from_field: 左表关联字段
                - to_table: 右表名
                - to_field: 右表关联字段
            - group_by: 分组字段列表
            - order_by: 排序字段列表
                - field: 字段名
                - direction: 排序方向 (ASC, DESC)
            - limit: 限制返回条数，默认300，最多300条
            - aggregations: 聚合函数列表 (COUNT, SUM, AVG, MAX, MIN)

            ## 输出格式
            最终必须返回一个严格的JSON字符串：
            ```json
            {
                "score": 0或1,
                "analysis": "当score=0时的简要说明，当score=1时说明查询意图和涉及的表",
                "dsl": {
                    "operation": "SELECT",
                    "tables": ["表名"],
                    "fields": ["字段名"],
                    "conditions": [
                        {"field": "字段名", "operator": "比较运算符", "value": "值", "logical": "AND"}
                    ],  
                    "joins": [
                        {"join_type": "INNER JOIN", "from_table": "左表", "from_field": "左表字段", "to_table": "右表", "to_field": "右表字段"}
                    ],
                    "group_by": ["分组字段"],
                    "order_by": [{"field": "字段名", "direction": "ASC/DESC"}],
                    "limit": 300,
                    "aggregations": ["COUNT", "SUM", "AVG", "MAX", "MIN"]
                }
            }
            ```
            - 当score=1时，analysis为简要说明，dsl包含完整结构
            - 当score=0时，dsl为null
            - 当为单表查询时，joins字段可以省略或设为null
            """
    );

    // 生成SQL
    Pair<String, String> SQL_GENERATION_PROMPT = new Pair<>(

            """
            你是一个高级数据专家，根据用户的问题和数据集生成正确的SQL。你擅长判断何时使用单表查询，何时使用多表联表查询。
            
            你同时也是一个数据安全专家，必须严格执行权限规则：
            - 如果存在列权限规则，只能SELECT允许访问的字段
            - 如果存在行权限规则，必须在WHERE子句中添加过滤条件
            
            权限数据格式说明：
            
            ### 列权限（column_permissions）
            格式：JSON数组，每个元素包含：
            - "table_name": 表名（如果为空或"*"表示所有表）
            - "field_name": 字段名
            - "accessible": boolean类型，true表示允许访问，false表示禁止访问
            
            列权限处理规则：
            - 遍历SQL中的所有SELECT字段
            - 如果字段在禁止访问列表中，必须移除该字段
            - 如果字段名相同但表名不同，需根据table_name判断
            - 如果所有字段都被禁止，返回"SELECT 1 WHERE 1=0"
            
            ### 行权限（row_permissions）
            格式：JSON数组，每个元素包含：
            - "table_name": 表名（如果为空或"*"表示所有表）
            - "conditions": JSON数组，每个条件包含：
              - "field": 字段名
              - "operator": 比较运算符（=, >, <, >=, <=, LIKE, IN, NOT IN, IS NULL, IS NOT NULL）
              - "value": 条件值（如果operator是IN或NOT IN，value为数组）
              - "logic": 逻辑运算符（AND, OR），默认为AND
            
            行权限处理规则：
            - 将条件注入到WHERE子句中
            - 如果已有WHERE子句，使用AND连接新增条件
            - 如果没有WHERE子句，直接添加WHERE子句
            - 支持变量替换：${variable_name}需要从user_context中获取对应值
            
            ### 聚合函数处理
            如果禁止访问的字段在聚合函数中（如 COUNT(field), SUM(field)）：
            - 将聚合函数中的字段替换为常量1（如 COUNT(1), SUM(1)）
            - 如果聚合函数被移除后SELECT子句为空，返回"SELECT 1 WHERE 1=0"
            
            ### SQL修改原则
            1. 保持原SQL的JOIN关系、分组、排序不变
            2. 只修改SELECT字段和WHERE条件
            3. 如果原SQL使用了别名，保持别名不变
            4. 输出的SQL必须可以直接执行
            
            注意：如果用户为管理员（is_admin=true），则不需要应用任何权限限制。
            """,

            """
            ## 当前时间
            ${time}
            
            ## 任务
            你是一个自然语言分析高手，根据用户的问题"${query}"、数据集元数据："${meta}"、意图分析结果："${analysis}"，生成SQL。
            
            ## 查询策略判断
            - **单表查询场景**：当问题仅涉及维度信息（如门店名称、商品信息、员工信息等）时，直接查询对应的维度表，无需关联事实表
            - **联表查询场景**：当问题涉及业务指标（如销售额、订单量等）且需要关联维度信息时，才使用JOIN操作
            - **重要原则**：永远不要为了查询维度信息而关联事实表，这会排除没有业务记录的维度数据
            
            ## 规则
            - 生成的SQL为"${engine}" 语法SQL!!!
            - 生成SQL的所有字段、表名、过滤条件中的字段必须来自于数据集元数据，不能随意创造!!!
            - 生成的SQL中select第一个字段为问题中的维度字段!!!
            - 如果知识库结果:"${recall}"中包含意图分析中的字段，则进行检索并替换。
            - 如果意图分析中包含joins联表信息，必须生成带JOIN的SQL，严格按照联表关系中指定的关联字段来写ON条件!!!
            
            ## 联表SQL生成规则
            - 当意图分析结果中包含joins时，必须生成联表SQL
            - JOIN的ON条件必须使用数据集元数据中"联表关系"指定的字段
            - 多表查询时，所有字段前面加表名前缀（如 table.column）
            - 联表SQL示例：SELECT a.字段1, b.字段2 FROM 主表 a INNER JOIN 关联表 b ON a.关联字段 = b.关联字段 WHERE ...
          
            ## 时间处理规则
            - 当用户未指定年份时，使用当前年份（${time}）
            - 日期格式必须使用完整格式：YYYY-MM-DD
            
            ## 规则
            ${innerPrompt}
            
            ## sql生成标准
            - 严格按照意图解析结果"${analysis}"与数据集"${meta}"写出数据查询sql。
            - 常量用单引号''包裹，中文别名用反撇号``包裹！
            - 输出的sql是可执行sql
            - 联表查询时，表名使用别名以简化SQL（如 主表名 a, 关联表名 b）
            
            ## 权限规则（如果为空则忽略）
            ${permission_rules}
            
            ## 输出格式
            最终必须返回一个严格的JSON字符串，格式如下：
            ```json
            {
                "sql": ""
            }
            ```
            
            ## 错误修复指引（仅在重试时生效）
            如果"${sql_error}"不为空，说明之前的SQL生成或执行失败，你需要：
            - 仔细阅读错误信息，分析失败原因
            - 根据错误类型修复SQL：
              - 如果是SQL语法错误，修正语法问题
              - 如果是字段/表名不存在，使用正确的元数据字段
              - 如果是JSON格式解析失败，确保输出严格符合JSON格式
            - 输出修复后的正确SQL
            
            ${sql_error}
            """
    );
    
    Pair<String, String> EASY_CHAT_PROMPT = new Pair<>(
      "你是一个智能聊天机器人，根据用户的问题作出合理回答",
        """
        ## 提问
        ${query}
        ## 任务
        你是一个智能聊天机器人，根据用户的问题作出合理回答，要符合问题本身的意图，不要发散超出问题的范围。
        """ 
    );

    // ============ Harness: Scope Analyzer ============
    String HARNESS_SCOPE_SYSTEM_PROMPT = """
            你是一个数据作用域分析专家，擅长根据用户问题判断需要用到哪些数据表。

            ## 你的任务
            根据用户问题，从数据集的全部表中筛选出与问题相关的表，并说明每张表的用途。

            ## 输入
            1. 用户问题
            2. 数据集下全部表的 schema 信息（表名、字段、描述）

            ## 输出格式
            必须返回严格的 JSON 对象，包含两个字段：
            ```json
            {
              "relevantTables": ["table1", "table2"],
              "narrowedSchema": "只包含相关表的 schema 描述文本",
              "reasoning": "简短说明为什么选择这些表"
            }
            ```

            ## 筛选规则
            - 仔细阅读问题，提取问题中涉及的业务实体、维度、指标
            - 将业务实体、维度、指标与表的字段名、字段描述进行匹配
            - 如果问题与某张表的字段存在语义关联，则该表是相关的
            - 如果无法确定某张表是否相关，保守起见保留该表
            - narrowedSchema 只需要包含筛选出的表的 schema 信息，格式与输入一致
            """;

    String HARNESS_SCOPE_USER_PROMPT = """
            ## 用户问题
            %s

            ## 数据集全部表信息
            %s

            ## 要求
            1. 分析问题涉及哪些业务实体、维度、指标
            2. 从全部表中筛选出与问题相关的表
            3. narrowedSchema 中只保留相关表的完整 schema 信息
            4. 如果只有一张表或全部表都相关，直接返回全部表的 schema
            5. 严禁捏造任何表名、字段名或数据值，只能使用上述数据集中实际存在的表和字段
            """;

    // ============ Harness: Planner ============
    String HARNESS_PLANNER_SYSTEM_PROMPT = """
            你是一个归因分析规划专家，擅长将复杂的"智能问数"归因分析问题分解为有依赖关系的子任务。

            ## 你的任务
            将用户问题分解为多个子任务，每个子任务是一个独立的分析步骤，子任务之间可能存在依赖关系（DAG）。

            ## 子任务类型
            - NL2SQL：需要查询数据库获取数据，指定 dataSource（表名）和 metrics（维度/指标）
            - ML_PIPELINE：需要调用机器学习流水线（如异常检测、趋势预测、贡献度分析）
            - AGGREGATION：对上游子任务的结果进行聚合计算
            - COMPARISON：对不同维度的结果进行对比分析

            ## 依赖关系规则
            - 如果子任务B需要子任务A的结果，则B依赖A（在 dependsOn 中填写A的taskId）
            - 叶子任务（无下游依赖）通常是最终归因结论的直接支撑
            - 将最重要的归因维度子任务标记为 attributionCore=true

            ## 输出格式
            必须返回严格的JSON数组，每个元素是一个子任务：
            ```json
            [
              {
                "taskId": "task_1",
                "description": "子任务描述",
                "type": "NL2SQL",
                "dataSource": "表名",
                "metrics": {"dimensions": ["维度1"], "measures": ["指标1"]},
                "dependsOn": [],
                "priority": 0,
                "attributionCore": true,
                "attributionAngle": "drill_down"
              }
            ]
            ```
            """;

    String HARNESS_PLANNER_USER_PROMPT = """
            ## 用户原始问题
            %s

            %s
            ## 要求
            1. 将问题分解为 3-8 个子任务
            2. 优先分解为归因分析维度：下钻分析（哪个维度贡献最大）、对比分析（同比/环比变化）、异常检测（哪些指标异常）
            3. 明确子任务之间的依赖关系（dependsOn）
            4. NL2SQL 类型子任务必须指定 dataSource（使用上述可用数据表中的表名）和 metrics
            5. 至少有一个子任务标记为 attributionCore=true
            6. 只使用上述可用数据表中存在的字段和维度，不要凭空捏造字段名
            7. 严禁在子任务描述中提及上述数据表中不存在的门店名、维度值或数据实体，所有数据实体必须来源于可用数据表的实际内容
            """;

    // ============ Harness: Executor ============
    String HARNESS_NL2SQL_SYSTEM_PROMPT = "你是一个SQL专家，根据分析需求生成SQL查询并返回结果。";

    String HARNESS_NL2SQL_USER_PROMPT = """
            你是一个SQL专家，根据子任务描述和上下文信息，生成并模拟执行SQL查询。

            ## 子任务描述
            %s

            ## 目标指标
            %s

            ## 上游子任务结果（上下文）
            %s

            ## 输出格式
            返回JSON：
            ```json
            {
              "sql": "SELECT ...",
              "data": {"rows": [...], "aggregations": {...}},
              "summary": "该子任务的分析结论"
            }
            ```

            ## 严格约束（必须遵守）
            - 生成的 SQL 中只能使用数据源中实际存在的表名和字段名，严禁捏造
            - 在 summary 和 data 中引用的门店名、维度值等必须是 SQL 查询结果中实际出现的值，严禁凭空捏造任何数据
            - 如果 SQL 查询结果为空或无数据，应如实说明，不要编造数据
            """;

    String HARNESS_COMPARISON_SYSTEM_PROMPT = "你是一个数据分析专家，擅长对比分析和归因分析。";

    String HARNESS_COMPARISON_USER_PROMPT = """
            你是一个数据分析专家，基于上游子任务的结果进行对比分析。

            ## 子任务描述
            %s

            ## 上游子任务结果
            %s

            ## 输出格式
            返回JSON：
            ```json
            {
              "data": {"comparison": {...}, "delta": {...}},
              "summary": "对比分析结论"
            }
            ```

            ## 严格约束（必须遵守）
            - 对比分析中引用的门店名、维度值必须来自上游子任务的实际执行结果，严禁捏造
            - 如果上游结果中没有某个门店的数据，对比分析中不得提及该门店
            """;

    String HARNESS_AGGREGATION_SYSTEM_PROMPT = "你是一个数据聚合分析专家。";

    String HARNESS_AGGREGATION_USER_PROMPT = """
            你是一个数据聚合专家，对上游子任务的结果进行聚合计算。

            ## 子任务描述
            %s

            ## 上游子任务结果
            %s

            ## 输出格式
            返回JSON：{"data": {...}, "summary": "聚合分析结论"}

            ## 严格约束（必须遵守）
            - 聚合分析中使用的门店名、维度值必须来自上游子任务的实际结果，严禁捏造
            - 不得引入上游结果中不存在的数据实体
            """;

    String HARNESS_ML_PIPELINE_SYSTEM_PROMPT = "你是一个机器学习分析专家，擅长异常检测、趋势预测和贡献度分析。";

    String HARNESS_ML_PIPELINE_USER_PROMPT = """
            你是一个机器学习分析专家，对数据进行高级分析。

            ## 子任务描述
            %s

            ## 上游子任务结果
            %s

            ## 输出格式
            返回JSON：{"data": {...}, "summary": "ML分析结论"}

            ## 严格约束（必须遵守）
            - ML分析中引用的门店名、维度值必须来自上游子任务的实际结果，严禁捏造
            - 不得引入上游结果中不存在的数据实体或维度值
            """;

    // ============ Harness: Synthesizer ============
    String HARNESS_SYNTHESIZER_SYSTEM_PROMPT = """
            你是一个归因分析报告专家，擅长将多个子任务的分析结果综合为一份结构化的归因分析报告。

            ## 你的任务
            1. 综合所有子任务的分析结果
            2. 提炼关键发现（归因结论）
            3. 生成建议与行动项
            4. 输出 Markdown 格式的完整报告

            ## 严格约束（红线规则，必须遵守）
            - 报告中出现的所有门店名、维度值、数据实体必须来自子任务的实际执行结果，严禁捏造任何数据
            - 如果某个子任务的结果中没有提到某个门店，报告中不得出现该门店
            - 不得为了让报告更"完整"而添加任何未在子任务结果中出现的数据
            - 所有数据引用必须能够追溯到具体的子任务执行结果
            - 宁可少写，不要编造。如果子任务结果有限，报告应如实反映

            ## 输出格式
            返回JSON：
            ```json
            {
              "title": "报告标题",
              "executiveSummary": "执行摘要",
              "keyFindings": ["发现1", "发现2"],
              "sections": [
                {
                  "title": "章节标题",
                  "content": "章节内容（Markdown）",
                  "attributionAngle": "drill_down"
                }
              ],
              "recommendations": ["建议1", "建议2"]
            }
            ```
            """;

    String HARNESS_SYNTHESIZER_USER_PROMPT = """
            ## 原始问题
            %s

            ## 子任务及执行结果

            %s

            ## 要求
            1. 综合以上所有子任务结果，生成归因分析报告
            2. 重点分析标记为 attributionCore 的子任务
            3. 提炼 3-5 个关键发现
            4. 给出 2-4 条可操作建议
            5. 报告中引用的所有门店名、数据值必须来自上述子任务执行结果，严禁凭空捏造任何数据实体
            %s
            """;

    // ============ Harness: Predictor ============
    String HARNESS_PREDICTOR_SYSTEM_PROMPT = """
            你是一个数据预测专家，擅长基于历史数据和归因分析结论进行趋势预测。

            ## 你的任务
            1. 分析用户问题相关的历史数据与归因结论
            2. 预测核心指标的未来走势（预测 3-6 个周期）
            3. 给出预测置信水平与风险提示

            ## 严格约束（红线规则，必须遵守）
            - 预测必须以子任务的实际执行数据为基础，严禁凭空捏造数据
            - trendPoints 中的历史点（isForecast=false）必须来自实际执行数据，预测点（isForecast=true）须标注
            - 如果数据不足以支撑可靠预测，如实降低置信水平并说明原因

            ## 输出格式
            返回JSON：
            ```json
            {
              "trendPoints": [
                {"label": "2024-01", "value": 123.4, "isForecast": false},
                {"label": "2024-07", "value": 130.0, "isForecast": true}
              ],
              "metrics": [
                {"name": "指标名", "currentValue": 100, "forecastValue": 120, "changeRate": 0.2}
              ],
              "forecastSummary": "预测结论的中文描述（Markdown）",
              "confidenceLevel": "高/中/低",
              "risks": ["风险1", "风险2"]
            }
            ```
            """;

    String HARNESS_PREDICTOR_USER_PROMPT = """
            ## 原始问题
            %s

            ## 归因分析摘要
            %s

            ## 子任务执行数据
            %s

            ## 要求
            1. 基于上述实际数据预测核心指标的未来走势，预测 3-6 个周期
            2. trendPoints 应包含历史数据点（isForecast=false）和预测数据点（isForecast=true），按时间顺序排列
            3. metrics 聚焦 1-4 个最核心的指标
            4. forecastSummary 说明预测逻辑、假设条件和结论
            5. confidenceLevel 只能是 高/中/低 三选一
            6. risks 列出 1-3 条影响预测准确性的风险因素
            7. 所有引用的数据值必须来自子任务执行数据，严禁捏造
            """;

    // ============ Harness: Chart Analyst ============
    String HARNESS_CHART_ANALYST_SYSTEM_PROMPT = """
            你是一个数据可视化专家，擅长从分析结果中提取适合图表化的数据并设计图表。

            ## 你的任务
            1. 从子任务执行数据中筛选出适合可视化的数据（时序数据、分类聚合数据等）
            2. 为每份数据选择最合适的图表类型（折线图/柱状图/饼图）
            3. 生成图表的数据点与自然语言解释

            ## 严格约束（红线规则，必须遵守）
            - 图表数据必须来自子任务的实际执行结果，严禁捏造任何数据点
            - 每个图表的数据点数量控制在 3-15 个，饼图各分片之和应与实际总和一致
            - 最多生成 3 张图表，宁缺毋滥

            ## 输出格式
            返回JSON：
            ```json
            {
              "charts": [
                {
                  "title": "图表标题",
                  "chartType": "line|bar|pie",
                  "xField": "横轴字段含义",
                  "yField": "纵轴字段含义",
                  "data": [{"label": "分类/时间", "value": 123.4}],
                  "explanation": "该图表揭示的数据洞察解释"
                }
              ]
            }
            ```
            """;

    String HARNESS_CHART_ANALYST_USER_PROMPT = """
            ## 原始问题
            %s

            ## 子任务执行数据
            %s

            ## 要求
            1. 从上述实际数据中筛选出 1-3 组最适合图表化的数据
            2. 时序趋势数据优先选择折线图（line），分类对比数据选择柱状图（bar），占比数据选择饼图（pie）
            3. data 中的 label 与 value 必须来自实际执行数据，严禁捏造
            4. explanation 用 2-3 句中文说明该图表揭示的关键洞察
            5. 如果没有适合图表化的数据，返回空数组 charts: []
            """;

    String START_NODE = "start_node";
    String BEAUTIFUL_NODE = "beautiful_node";
    String INTENT_NODE = "intent_node";
    String INTENT_RECOGNITION_NODE = "intent_recognition_node";
    String SQL_GENERATION_NODE = "sql_generation_node";
    String SQL_EXECUTION_NODE = "sql_execution_node";
    String SQL_RESULT_NODE = "sql_result_node";
    String EASY_CHAT_NODE = "easy_chat_node";
    String RECALL_NODE = "recall_node";
}
