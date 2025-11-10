package com.datacopilotx.ai.domian.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionLogDTO {
    @JsonProperty("session_id")
    private String sessionId;
    private String question;
    private String ctime;
}
