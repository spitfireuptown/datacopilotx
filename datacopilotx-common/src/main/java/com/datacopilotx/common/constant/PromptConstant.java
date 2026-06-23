package com.datacopilotx.common.constant;

import cn.hutool.core.lang.Pair;

// key: system prompt
// value: user prompt
public interface PromptConstant {

    // 美化提问
    Pair<String, String> BEAUTIFUL_PROMPT = new Pair<>(
            "你是一个自然语言分析高手，美化用户的提问问题",
            """
            ## 任务
            - 你是一个自然语言分析高手，美化用户的提问'${query}'，根据问题语义保证在不改变语义的基础上，优化倒装、错别字、口语化等问题。
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
            你是一个自然语言分析高手，判断用户问题"${query}"的意图，特别擅长分析多表联表查询场景。
            """,

            """
            ## 任务
            分析用户问题中的意图，判断用户的问题:"${query}"是否想要查询数据集:"${meta}"中的数据。
            如果是，则解析出完整的查询DSL结构。
            
            **重要：如果问题涉及多个表的字段（如订单表+明细表、用户表+订单表），你必须使用joins进行联表查询！**

            ## 数据集Schema
            ${meta}

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
            - joins: 关联查询信息（多表关联时需要）
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
            """
    );

    // 生成SQL
    Pair<String, String> SQL_GENERATION_PROMPT = new Pair<>(

            "你是一个高级数据专家擅长构造SQL，根据用户的问题和数据集，生成SQL，特别擅长多表联表查询",

            """
            ## 当前时间
            ${time}
            
            ## 任务
            你是一个自然语言分析高手，根据用户的问题"${query}"、数据集元数据："${meta}"、意图分析结果："${analysis}"，生成SQL。
            
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
            
            ## 规则
            ${innerPrompt}
            
            ## sql生成标准
            - 严格按照意图解析结果"${analysis}"与数据集"${meta}"写出数据查询sql。
            - 常量用单引号''包裹，中文别名用反撇号``包裹！
            - 输出的sql是可执行sql
            - 联表查询时，表名使用别名以简化SQL（如 主表名 a, 关联表名 b）
            
            ## 输出格式
            最终必须返回一个严格的JSON字符串，格式如下：
            ```json
            {
                "sql": ""
            }
            ```
            
            ## SQL修复（仅在重试时生效）
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
