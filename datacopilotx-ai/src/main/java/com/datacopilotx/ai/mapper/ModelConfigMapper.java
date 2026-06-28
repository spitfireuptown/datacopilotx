package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfigBean> {
}