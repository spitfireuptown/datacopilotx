package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacopilotx.ai.domian.bean.DatasetRelationBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DatasetRelationMapper extends BaseMapper<DatasetRelationBean> {
    
    /**
     * 根据数据集ID查询所有关联关系
     */
    List<DatasetRelationBean> selectByDatasetId(@Param("datasetId") Long datasetId);
    
    /**
     * 删除指定数据集的所有关联关系
     */
    int deleteByDatasetId(@Param("datasetId") Long datasetId);
}
