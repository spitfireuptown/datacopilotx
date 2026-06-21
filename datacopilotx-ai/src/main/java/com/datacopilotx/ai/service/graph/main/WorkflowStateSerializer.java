package com.datacopilotx.ai.service.graph.main;

import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;

public class WorkflowStateSerializer extends ObjectStreamStateSerializer<WorkflowState> {

    public WorkflowStateSerializer() {
        super(WorkflowState::new);
    }
}
