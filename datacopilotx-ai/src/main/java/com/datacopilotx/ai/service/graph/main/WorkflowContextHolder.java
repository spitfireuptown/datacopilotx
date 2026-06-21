package com.datacopilotx.ai.service.graph.main;

import com.datacopilotx.common.result.WebResult;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

/**
 * 用于 SerializableSink 序列化/反序列化时传递 Sinks.Many
 */
public class WorkflowContextHolder {

    private static final ThreadLocal<Sinks.Many<ServerSentEvent<WebResult<String>>>> SINK_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);
    
    public static void setSerializableSink(SerializableSink serializableSink) {
        SINK_THREAD_LOCAL.set(serializableSink.getSink());
    }
    
    public static Sinks.Many<ServerSentEvent<WebResult<String>>> getAndClearSerializableSink() {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = SINK_THREAD_LOCAL.get();
        SINK_THREAD_LOCAL.remove();
        return sink;
    }
}
