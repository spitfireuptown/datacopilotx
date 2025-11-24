package com.datacopilotx.ai.service.flow;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.common.result.WebResult;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 流程服务辅助类，提供公共方法
 */
@Component
public class WorkflowServiceHelper {
    
    // 替换prompt占位符
    public Pair<String, String> injectPrompt(Pair<String, String> promptPair, Map<String, String> params) {
        String systemPrompt = promptPair.getKey();
        String userPrompt = promptPair.getValue();

        for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
            systemPrompt = systemPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
            userPrompt = userPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        return new Pair<>(systemPrompt, userPrompt);
    }

    // 组装数据集信息
    public String assembleDataSetInfo(DataSetBean dataSetBean) {
        List<DataSetDTO.SchemaInfo> schemaInfos = JSONUtil.toList(dataSetBean.getFields(), DataSetDTO.SchemaInfo.class);

        StringBuilder stringBuilder = new StringBuilder();
        for (DataSetDTO.SchemaInfo field : schemaInfos) {
            stringBuilder.append(
                    String.format("    - 字段名称：%s 字段类型：%s 字段描述：%s \n", field.getFieldName(), field.getFieldType(), field.getDescription())
            );
        }

        return String.format("""
                \n
                **数据表名:** %s
                **数据表描述:** %s
                **数据表字段描述:** \n %s
                """, dataSetBean.getTable(), dataSetBean.getDescription(), stringBuilder);
    }

    // 获取当前时间
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 输出问题和答案（内部方法）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }

    // 输出问题和答案（内部方法）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, QuestionForm questionForm) {
        questionForm.setAnswer(ObjectUtils.isEmpty(questionForm.getAnswer()) ? content : questionForm.getAnswer() + content);
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    // 异常处理
    public void errorHandling(String node, Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String errorMsg) {
        ServerSentEvent<WebResult<String>> build = buildSseEvent(node, WebResult.error(500, errorMsg));
        sink.emitNext(build, Sinks.EmitFailureHandler.FAIL_FAST);
        sink.tryEmitComplete();
    }
    
    // 构建SSE事件
    public ServerSentEvent<WebResult<String>> buildSseEvent(String node, WebResult<String> data) {
        return ServerSentEvent.<WebResult<String>>builder()
                .id(node)
                .data(data)
                .build();
    }
}