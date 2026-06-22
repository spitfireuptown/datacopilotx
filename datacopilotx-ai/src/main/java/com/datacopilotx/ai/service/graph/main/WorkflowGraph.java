package com.datacopilotx.ai.service.graph.main;

import com.datacopilotx.ai.service.graph.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@RequiredArgsConstructor
@Component
public class WorkflowGraph {

    private static final int MAX_RETRY = 3;

    private final GracefulQuestionGraphNode gracefulQuestionGraphNode;
    private final IntentRecognitionGraphNode intentRecognitionGraphNode;
    private final EasyChatGraphNode easyChatGraphNode;
    private final RecallKnowledgeGraphNode recallKnowledgeGraphNode;
    private final GenerateSqlGraphNode generateSqlGraphNode;
    private final ExecuteSQLGraphNode executeSQLGraphNode;

    public StateGraph<WorkflowState> createResearchGraph() throws GraphStateException {
        log.info("Creating deep research main graph...");

        StateGraph<WorkflowState> workflow = new StateGraph<>(WorkflowState.SCHEMA, new WorkflowStateSerializer())
                .addNode("graceful_question", node_async(gracefulQuestionGraphNode))
                .addNode("intent_recognition", node_async(intentRecognitionGraphNode))
                .addNode("easy_chat", node_async(easyChatGraphNode))
                .addNode("recall_knowledge", node_async(recallKnowledgeGraphNode))
                .addNode("generate_sql", node_async(generateSqlGraphNode))
                .addNode("execute_sql", node_async(executeSQLGraphNode))

                .addEdge(START, "graceful_question")
                .addEdge("graceful_question", "intent_recognition")
                .addEdge("recall_knowledge", "generate_sql")
                .addEdge("generate_sql", "execute_sql")
                .addConditionalEdges(
                        "intent_recognition",
                        edge_async(state -> {
                            Integer intentScore = state.intentScore().orElse(0);
                            if (intentScore == 1) {
                                return "recall_knowledge";
                            } else {
                                return "easy_chat";
                            }
                        }),
                        Map.of("recall_knowledge", "recall_knowledge", "easy_chat", "easy_chat")
                )
                .addConditionalEdges(
                        "execute_sql",
                        edge_async(state -> {
                            String sqlError = state.sqlError().orElse("");
                            int retryCount = state.retryCount().orElse(0);
                            if (!sqlError.isEmpty() && retryCount < MAX_RETRY) {
                                return "generate_sql";
                            }
                            return END;
                        }),
                        Map.of("generate_sql", "generate_sql", END, END)
                )
                .addEdge("easy_chat", END);

        log.info("Main graph creation completed");
        return workflow;
    }

    public Map<String, Object> createInitialState(String sessionId, String questionId, Long datasetId, Long modelId, String question) {
        return WorkflowState.createInitialState(sessionId, questionId, datasetId, modelId, question);
    }
}
