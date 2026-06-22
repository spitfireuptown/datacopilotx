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

    // 判断是否需要生成SQL
    Pair<String, String> INTENT_RECOGNITION_PROMPT = new Pair<>(
            """
            你是一个自然语言分析高手，判断用户问题"${query}"的意图
            """,

            """
            ## 任务
            分析用户问题中的意图，判断用户的问题:"${query}"是否想要查询数据集:"${meta}"中的数据
            - 分析出用户提问中的维度字段、指标字段、过滤条件、排序字段等信息写入到analysis中
            - 格式为字符串类型，例:"维度字段:xxx;指标字段:xxx;过滤条件:xxx;排序字段:xxx;"
            - 如果用户问题中没有维度字段、指标字段、过滤条件、排序字段等信息，则score为0，将用合理的回答写入analysis""
            - 如果用户包含维度字段、指标字段、过滤条件、排序字段写写入analysis，score为1
            
            ## 输出格式
            最终必须返回一个严格的JSON字符串，格式如下：
            ```json
            {
                "score": 0,
                "analysis": ""
            }
            ```
            """
    );

    // 生成SQL
    Pair<String, String> SQL_GENERATION_PROMPT = new Pair<>(

            "你是一个高级数据专家擅长构造SQL，根据用户的问题和数据集，生成SQL",

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
            
            ## 规则
            ${innerPrompt}
            
            ## sql生成标准
            - 严格按照意图解析结果"${analysis}"与数据集"${meta}"写出数据查询sql。
            - 常量用单引号''包裹，中文别名用反撇号``包裹！
            - 输出的sql是可执行sql
            
            ## 输出格式
            最终必须返回一个严格的JSON字符串，格式如下：
            ```json
            {
                "sql": ""
            }
            ```
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
