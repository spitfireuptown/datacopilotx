package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacopilotx.ai.controller.form.ChatForm;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.QuestionLogDTO;
import com.datacopilotx.ai.domian.vo.PageVO;
import com.datacopilotx.ai.domian.vo.QuestionDetailLogVO;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatBusinessService {

    @Resource
    QuestionLogMapper questionLogMapper;

    public PageVO<List<QuestionLogDTO>> chatHistory(ChatForm chatForm) {
        PageVO<List<QuestionLogDTO>> result = new PageVO<>();
        IPage<QuestionLogDTO> queryLogDTOIPage = questionLogMapper. selectQueryLog(new Page<>(chatForm.getPageNo(), chatForm.getPageSize()), chatForm.getSearchkey());
        result.setPageNo(chatForm.getPageNo());
        result.setPageSize(chatForm.getPageSize());
        result.setTotal(queryLogDTOIPage.getTotal());
        result.setTotalPage(queryLogDTOIPage.getPages());
        result.setData(queryLogDTOIPage.getRecords());
        return result;
    }


    public List<QuestionDetailLogVO> chatHistoryDetail(ChatForm chatForm) {
            return questionLogMapper.selectList(
                new LambdaQueryWrapper<QuestionLogBean>()
                        .eq(QuestionLogBean::getSessionId, chatForm.getSessionId())
                        .orderByAsc(QuestionLogBean::getCtime)
        ).stream().map(QuestionDetailLogVO::convert).toList();
    }

    public void deleteChatHistory(String id) {
        this.questionLogMapper.delete(new LambdaQueryWrapper<QuestionLogBean>().eq(QuestionLogBean::getSessionId, id));
    }
}
