package com.datacopilotx.ai.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.ModelForm;
import com.datacopilotx.ai.domian.vo.ModelConfigVO;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.GlobalConstant;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.ResponseCode;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.domian.dto.ModelConfigDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModelConfigService {

    @Resource
    ModelConfigMapper mapper;
    @Autowired
    AIGatewayChatService aiGatewayChatService;


    public List<ModelConfigVO.List> list(String name, String type) {
        return mapper.selectList(
                new LambdaQueryWrapper<ModelConfigBean>()
                        .eq(!ObjectUtils.isEmpty(type), ModelConfigBean::getFunctionType, type)
                        .like(!ObjectUtils.isEmpty(name), ModelConfigBean::getModel, name)
        ).stream().map(bean -> {
            ModelConfigVO.List list = new ModelConfigVO.List();
            list.setId(bean.getId());
            list.setModel(bean.getModel());
            list.setApiBase(bean.getBaseUrl());
            list.setType(bean.getType());
            list.setFunctionType(bean.getFunctionType());
            list.setPlatform(bean.getPlatform());
            list.setApiKey(bean.getApiKey());

            LocalDateTime localDateTime = bean.getCtime().toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            list.setCreateTime(localDateTime.format(formatter));

            list.setDimension(bean.getDimension());
            return list;
        }).collect(Collectors.toList());
    }

    public Long create(ModelForm.Create modelForm) {
        this.verifyModelConfig(modelForm);
        ModelConfigBean bean = new ModelConfigBean();
        bean.setModel(modelForm.getModel());
        bean.setApiKey(modelForm.getApiKey());
        bean.setBaseUrl(modelForm.getApiBase());
        bean.setPlatform(modelForm.getPlatform());
        bean.setType(modelForm.getType());
        bean.setDimension(modelForm.getDimension());
        bean.setFunctionType(modelForm.getFunctionType());
        mapper.insert(bean);
        return bean.getId();
    }

    public Long modify(ModelForm.Create modelForm) {
        this.verifyModelConfig(modelForm);
        ModelConfigBean bean = mapper.selectOne(new LambdaQueryWrapper<ModelConfigBean>().eq(ModelConfigBean::getId, modelForm.getId()));
        if (ObjectUtils.isEmpty(bean)) {
            throw new DataCopilotXException(ResponseCode.MODEL_NOT_FOUND);
        }
        bean.setModel(modelForm.getModel());
        bean.setApiKey(modelForm.getApiKey());
        bean.setBaseUrl(modelForm.getApiBase());
        bean.setPlatform(modelForm.getPlatform());
        bean.setType(modelForm.getType());
        bean.setDimension(modelForm.getDimension());
        bean.setFunctionType(modelForm.getFunctionType());
        mapper.updateById(bean);
        return bean.getId();
    }


    public void testConnection(long id) {
        ModelConfigBean modelConfigBean = mapper.selectOne(new LambdaQueryWrapper<ModelConfigBean>().eq(ModelConfigBean::getId, id));
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            throw new DataCopilotXException(ResponseCode.MODEL_NOT_FOUND);
        }
        this.testModelConn(
                ModelForm.Create.builder()
                        .model(modelConfigBean.getModel())
                        .apiKey(modelConfigBean.getApiKey())
                        .platform(modelConfigBean.getPlatform())
                        .type(modelConfigBean.getType())
                        .apiBase(modelConfigBean.getBaseUrl())
                        .build()
        );
    }


    private void verifyModelConfig(ModelForm.Create modelForm) {
        // 是否重复配置
        if (!ObjectUtils.isEmpty(modelForm.getModel())) {
            ModelConfigBean modelConfigBean = mapper.selectOne(new LambdaQueryWrapper<ModelConfigBean>().eq(ModelConfigBean::getModel, modelForm.getModel()));
            if (modelConfigBean != null && !modelConfigBean.getId().equals(modelForm.getId())) {
                throw new DataCopilotXException(ResponseCode.MODEL_REPETITION);
            }
        }
        this.testModelConn(modelForm);
    }

    private void testModelConn(ModelForm.Create modelForm) {
        // 校验模型连接情况
        try {
            if (GlobalConstant.CHAT_FUNCTION_TYPE.equals(modelForm.getFunctionType())) {
                aiGatewayChatService.chatCompletions(
                        ChatRequest.builder()
                                .model(modelForm.getModel())
                                .question("你好")
                                .apiKey(modelForm.getApiKey())
                                .platform(modelForm.getPlatform())
                                .type(modelForm.getType() )
                                .baseUrl(modelForm.getApiBase())
                                .build()
                );
            } else {
                aiGatewayChatService.embedding(
                        ChatRequest.builder()
                                .model(modelForm.getModel())
                                .question("你好")
                                .apiKey(modelForm.getApiKey())
                                .dimensions(modelForm.getDimension())
                                .platform(modelForm.getPlatform())
                                .type(modelForm.getType() )
                                .baseUrl(modelForm.getApiBase())
                                .build()
                );
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataCopilotXException(ResponseCode.MODEL_CONNECTION_FAILED);
        }
    }

    public void del(int id) {
        mapper.deleteById(id);
    }

}
