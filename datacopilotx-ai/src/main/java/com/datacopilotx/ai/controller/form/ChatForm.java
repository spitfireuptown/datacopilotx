package com.datacopilotx.ai.controller.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatForm {
    private Integer pageNo;
    private Integer pageSize;
    private String searchkey;
    private String sessionId;
}
