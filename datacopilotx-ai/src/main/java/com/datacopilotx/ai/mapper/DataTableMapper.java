package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacopilotx.ai.domian.bean.DataTableBean;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataTableMapper extends BaseMapper<DataTableBean> {
}