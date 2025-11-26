package com.datacopilotx.ai.service.graph;

import com.datacopilotx.common.result.WebResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.langchain4j.serializer.jackson.LC4jJacksonStateSerializer;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;


@Slf4j
@RequiredArgsConstructor
@Component
public class WorkflowGraphBuilder {
    private final GracefulQuestionGraphNode gracefulQuestionGraphNode;
    private final EasyChatGraphNode easyChatGraphNode;
    private final IntentRecognitionGraphNode intentRecognitionGraphNode;
    private final RecallKnowledgeGraphNode recallKnowledgeGraphNode;
    private final GenerateSqlGraphNode generateSqlGraphNode;
    private final ExecuteSQLGraphNode executeSQLGraphNode;


    public StateGraph<WorkflowState> createResearchGraph() throws GraphStateException {
        log.info("Creating deep research state graph...");

        var serializer = new LC4jJacksonStateSerializer<>(WorkflowState::new);
        serializer.objectMapper().registerModule(new JavaTimeModule());
        serializer.objectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

//        StateGraph<ResearchState> workflow = new StateGraph<>(ResearchState.SCHEMA, serializer)
        StateGraph<WorkflowState> workflow = new StateGraph<>(WorkflowState.SCHEMA, serializer)
                // Add research nodes
                .addNode("graceful_question", node_async(gracefulQuestionGraphNode))
                .addNode("intent_recognition", node_async(intentRecognitionGraphNode))
                .addNode("easy_chat", node_async(easyChatGraphNode))
                .addNode("recall_knowledge", node_async(recallKnowledgeGraphNode))
                .addNode("generate_sql", node_async(generateSqlGraphNode))
                .addNode("execute_sql", node_async(executeSQLGraphNode))

                // Set entry point: start with query generation
                .addEdge(START, "graceful_question")
                // Linear flow: graceful_question -> intent_recognition -> easy_chat -> recall_knowledge -> generate_sql -> execute_sql
                .addEdge("graceful_question", "intent_recognition")
                .addEdge("recall_knowledge", "generate_sql")
                .addEdge("generate_sql", "execute_sql")
                // Conditional routing edges: continue or end based on routing decision
                .addConditionalEdges(
                        "intent_recognition",
                        // Routing condition function: check if research should continue
                        edge_async(state -> {
                            // Convert AgentState to ResearchState to access convenience methods
                            WorkflowState researchState = new WorkflowState(state.data());
                            if (researchState.intentScore().orElse(0) == 1) {
                                return "recall_knowledge";
                            } else {
                                return "easy_chat";
                            }
                        }),
                        // Route mapping
                        Map.of(
                                "recall_knowledge", "recall_knowledge",  // Continue research: back to query generation
                                "easy_chat", "easy_chat"         // End research: enter finalization
                        )
                )
                // End after finalization
                .addEdge("execute_sql", END);

        log.info("Research state graph creation completed");
        return workflow;
    }


    public Map<String, Object> createInitialState(
            String sessionId,
            String questionId,
            Long datasetId,
            Long modelId,
            Sinks.Many<ServerSentEvent<WebResult<String>>> sink) {

        return WorkflowState.createInitialState(
                sessionId,
                questionId,
                datasetId,
                modelId,
                sink
        );
    }

}
