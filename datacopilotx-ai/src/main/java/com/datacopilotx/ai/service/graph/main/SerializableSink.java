package com.datacopilotx.ai.service.graph.main;

import com.datacopilotx.common.result.WebResult;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.io.Serializable;

/**
 * 可序列化的 Sink 包装类，用于在 WorkflowState 中持久化
 * Sinks.Many 本身不可序列化，所以需要包装
 */
public class SerializableSink implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // transient - 序列化时跳过，反序列化时从 ThreadLocal 恢复
    private transient volatile Sinks.Many<ServerSentEvent<WebResult<String>>> sink;
    
    public SerializableSink() {
    }
    
    public SerializableSink(Sinks.Many<ServerSentEvent<WebResult<String>>> sink) {
        this.sink = sink;
    }
    
    public void setSink(Sinks.Many<ServerSentEvent<WebResult<String>>> sink) {
        this.sink = sink;
    }
    
    public Sinks.Many<ServerSentEvent<WebResult<String>>> getSink() {
        return sink;
    }

    
    // 序列化时保存当前 sink 到 ThreadLocal（用于反序列化恢复）
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        WorkflowContextHolder.setSerializableSink(this);
        out.defaultWriteObject();
    }
    
    // 反序列化时从 ThreadLocal 恢复 sink
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.sink = WorkflowContextHolder.getAndClearSerializableSink();
    }
}
