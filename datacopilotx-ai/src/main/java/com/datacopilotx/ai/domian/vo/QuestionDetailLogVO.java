package com.datacopilotx.ai.domian.vo;

import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class QuestionDetailLogVO {
    private Long id;
    private String questionId;
    private String sessionId;
    private String question;
    private String sql;
    private String answer;
    private String result;
    private String ctime;
    private Long datasetId;
    private String dsName;
    private Long modelId;
    private String modelName;

    public static QuestionDetailLogVO convert(QuestionLogBean questionLogBean, String dsName, String modelName) {
        QuestionDetailLogVO questionDetailLogVO = new QuestionDetailLogVO();
        questionDetailLogVO.setId(questionLogBean.getId());
        questionDetailLogVO.setQuestionId(questionLogBean.getQuestionId());
        questionDetailLogVO.setSessionId(questionLogBean.getSessionId());
        questionDetailLogVO.setQuestion(questionLogBean.getQuestion());
        questionDetailLogVO.setSql(questionLogBean.getSql());
        questionDetailLogVO.setAnswer(questionLogBean.getAnswer());
        questionDetailLogVO.setResult(questionLogBean.getResult());
        questionDetailLogVO.setDatasetId(questionLogBean.getDatasetId());
        questionDetailLogVO.setDsName(dsName);
        questionDetailLogVO.setModelId(questionLogBean.getModelId());
        questionDetailLogVO.setModelName(modelName);

        LocalDateTime localDateTime = questionLogBean.getCtime().toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = localDateTime.format(formatter);

        questionDetailLogVO.setCtime(formattedDate);
        return questionDetailLogVO;
    }
}
