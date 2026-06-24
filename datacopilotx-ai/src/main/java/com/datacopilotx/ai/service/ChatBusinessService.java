package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacopilotx.ai.controller.form.ChatForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.QuestionLogDTO;
import com.datacopilotx.ai.domian.vo.PageVO;
import com.datacopilotx.ai.domian.vo.QuestionDetailLogVO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
public class ChatBusinessService {

    @Resource
    QuestionLogMapper questionLogMapper;

    @Resource
    DataSetMapper dataSetMapper;

    @Resource
    ModelConfigMapper modelConfigMapper;

    public PageVO<List<QuestionLogDTO>> chatHistory(ChatForm chatForm) {
        PageVO<List<QuestionLogDTO>> result = new PageVO<>();
        // 获取当前用户信息用于权限控制
        String currentUserId = SecurityUtil.getCurrentUserId();

        IPage<QuestionLogDTO> queryLogDTOIPage = questionLogMapper.selectQueryLog(
            new Page<>(chatForm.getPageNo(), chatForm.getPageSize()), 
            chatForm.getSearchkey(),
            currentUserId
        );
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
        ).stream().map(questionLogBean -> {
            String dsName = null;
            if (!ObjectUtils.isEmpty(questionLogBean.getDatasetId())) {
                DataSetBean dataSetBean = dataSetMapper.selectById(questionLogBean.getDatasetId());
                if (dataSetBean != null) {
                    dsName = dataSetBean.getDsName();
                }
            }

            String modelName = null;
            if (!ObjectUtils.isEmpty(questionLogBean.getModelId())) {
                ModelConfigBean modelConfigBean = modelConfigMapper.selectById(questionLogBean.getModelId());
                if (modelConfigBean != null) {
                    modelName = modelConfigBean.getModel();
                }
            }

            return QuestionDetailLogVO.convert(questionLogBean, dsName, modelName);
        }).toList();
    }

    public void deleteChatHistory(String id) {
        this.questionLogMapper.delete(new LambdaQueryWrapper<QuestionLogBean>().eq(QuestionLogBean::getSessionId, id));
    }
}
