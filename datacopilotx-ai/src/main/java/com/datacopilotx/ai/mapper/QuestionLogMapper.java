package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.QuestionLogDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionLogMapper extends BaseMapper<QuestionLogBean> {

    IPage<QuestionLogDTO> selectQueryLog(IPage<QuestionLogDTO> page, String searchKey);
}
