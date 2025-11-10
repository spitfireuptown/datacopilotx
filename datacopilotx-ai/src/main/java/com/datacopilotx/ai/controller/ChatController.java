package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.controller.form.ChatForm;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.dto.QuestionLogDTO;
import com.datacopilotx.ai.domian.vo.PageVO;
import com.datacopilotx.ai.domian.vo.QuestionDetailLogVO;
import com.datacopilotx.ai.service.ChatBusinessService;
import com.datacopilotx.ai.service.ChatService;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    ChatBusinessService chatBusinessService;
    @Autowired
    ChatService chatService;


    @RequestMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WebResult<String>>> chatCompletions(@RequestBody QuestionForm questionForm) {
        return chatService.chatCompletions(questionForm);
    }

    @RequestMapping(value = "/history")
    public WebResult<PageVO<List<QuestionLogDTO>>> chatHistory(ChatForm chatForm) {
        return WebResult.success(chatBusinessService.chatHistory(chatForm));
    }

    @RequestMapping(value = "/history/detail")
    public WebResult<List<QuestionDetailLogVO>> chatHistoryDetail(ChatForm chatForm) {
        return WebResult.success(chatBusinessService.chatHistoryDetail(chatForm));
    }

    @DeleteMapping(value = "/delete/{id}")
    public void deleteChatHistory(@PathVariable("id") String id) {
        this.chatBusinessService.deleteChatHistory(id);
    }
}
