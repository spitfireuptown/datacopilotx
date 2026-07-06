package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DataTableBean;
import com.datacopilotx.ai.mapper.DataTableMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DataTableService {

    @Autowired
    DataTableMapper dataTableMapper;

    public List<DataTableBean> listByDatasetId(Long datasetId) {
        return dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, datasetId)
                .orderByAsc(DataTableBean::getCtime));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DataTableBean dataTableBean) {
        dataTableMapper.insert(dataTableBean);
        return dataTableBean.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long update(DataTableBean dataTableBean) {
        dataTableMapper.updateById(dataTableBean);
        return dataTableBean.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void del(Long id) {
        dataTableMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delByDatasetId(Long datasetId) {
        dataTableMapper.delete(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, datasetId));
    }

    public DataTableBean detail(Long id) {
        return dataTableMapper.selectById(id);
    }
}