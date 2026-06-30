package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.controller.form.DatasetRelationForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.DatasetRelationBean;
import com.datacopilotx.ai.domian.vo.DatasetRelationVO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.DatasetRelationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatasetRelationService {

    @Autowired
    private DatasetRelationMapper datasetRelationMapper;

    @Autowired
    private DataSetMapper dataSetMapper;

    public List<DatasetRelationVO.ListVO> list() {
        List<DatasetRelationBean> relationList = datasetRelationMapper.selectList(new QueryWrapper<>());
        
        List<Long> allDatasetIds = relationList.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getFromDatasetId(), r.getToDatasetId()))
                .distinct()
                .collect(Collectors.toList());
        
        List<DataSetBean> dataSetList = allDatasetIds.isEmpty() ? java.util.Collections.emptyList() :
                dataSetMapper.selectBatchIds(allDatasetIds);
        
        Map<Long, String> dataSetMap = dataSetList.stream()
                .collect(Collectors.toMap(DataSetBean::getId, DataSetBean::getDsName, (v1, v2) -> v1));
        
        return relationList.stream().map(bean -> {
            DatasetRelationVO.ListVO list = new DatasetRelationVO.ListVO();
            list.setId(bean.getId());
            list.setFromDatasetId(bean.getFromDatasetId());
            list.setFromDatasetName(dataSetMap.get(bean.getFromDatasetId()));
            list.setFromField(bean.getFromField());
            list.setToDatasetId(bean.getToDatasetId());
            list.setToDatasetName(dataSetMap.get(bean.getToDatasetId()));
            list.setToField(bean.getToField());
            list.setRelationType(bean.getRelationType());
            list.setDescription(bean.getDescription());
            return list;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DatasetRelationForm.Create createForm) {
        DatasetRelationBean bean = new DatasetRelationBean();
        bean.setFromDatasetId(createForm.getFromDatasetId());
        bean.setFromField(createForm.getFromField());
        bean.setToDatasetId(createForm.getToDatasetId());
        bean.setToField(createForm.getToField());
        bean.setRelationType(createForm.getRelationType());
        bean.setDescription(createForm.getDescription());
        bean.setCtime(new Timestamp(System.currentTimeMillis()));
        bean.setUtime(new Timestamp(System.currentTimeMillis()));
        bean.setIsDel(0);

        datasetRelationMapper.insert(bean);
        return bean.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long update(DatasetRelationForm.Create updateForm) {
        DatasetRelationBean bean = datasetRelationMapper.selectById(updateForm.getId());
        if (bean == null) {
            throw new RuntimeException("关联关系不存在");
        }

        bean.setFromDatasetId(updateForm.getFromDatasetId());
        bean.setFromField(updateForm.getFromField());
        bean.setToDatasetId(updateForm.getToDatasetId());
        bean.setToField(updateForm.getToField());
        bean.setRelationType(updateForm.getRelationType());
        bean.setDescription(updateForm.getDescription());
        bean.setUtime(new Timestamp(System.currentTimeMillis()));

        datasetRelationMapper.updateById(bean);
        return bean.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void del(Long id) {
        datasetRelationMapper.deleteById(id);
    }

    public DatasetRelationVO.DetailVO detail(Long id) {
        DatasetRelationBean bean = datasetRelationMapper.selectById(id);
        if (bean == null) {
            return null;
        }

        List<Long> datasetIds = java.util.Arrays.asList(bean.getFromDatasetId(), bean.getToDatasetId());
        List<DataSetBean> dataSetList = dataSetMapper.selectBatchIds(datasetIds);
        
        Map<Long, String> dataSetMap = dataSetList.stream()
                .collect(Collectors.toMap(DataSetBean::getId, DataSetBean::getDsName, (v1, v2) -> v1));

        DatasetRelationVO.DetailVO detail = new DatasetRelationVO.DetailVO();
        detail.setId(bean.getId());
        detail.setFromDatasetId(bean.getFromDatasetId());
        detail.setFromDatasetName(dataSetMap.get(bean.getFromDatasetId()));
        detail.setFromField(bean.getFromField());
        detail.setToDatasetId(bean.getToDatasetId());
        detail.setToDatasetName(dataSetMap.get(bean.getToDatasetId()));
        detail.setToField(bean.getToField());
        detail.setRelationType(bean.getRelationType());
        detail.setDescription(bean.getDescription());
        detail.setCtime(bean.getCtime());
        detail.setUtime(bean.getUtime());
        return detail;
    }

    public List<DatasetRelationVO.ListVO> listByDatasetId(Long datasetId) {
        List<DatasetRelationBean> relationList = datasetRelationMapper.selectByDatasetId(datasetId);
        
        List<Long> allDatasetIds = relationList.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getFromDatasetId(), r.getToDatasetId()))
                .distinct()
                .collect(Collectors.toList());
        
        List<DataSetBean> dataSetList = allDatasetIds.isEmpty() ? java.util.Collections.emptyList() :
                dataSetMapper.selectBatchIds(allDatasetIds);
        
        Map<Long, String> dataSetMap = dataSetList.stream()
                .collect(Collectors.toMap(DataSetBean::getId, DataSetBean::getDsName, (v1, v2) -> v1));
        
        return relationList.stream().map(bean -> {
            DatasetRelationVO.ListVO list = new DatasetRelationVO.ListVO();
            list.setId(bean.getId());
            list.setFromDatasetId(bean.getFromDatasetId());
            list.setFromDatasetName(dataSetMap.get(bean.getFromDatasetId()));
            list.setFromField(bean.getFromField());
            list.setToDatasetId(bean.getToDatasetId());
            list.setToDatasetName(dataSetMap.get(bean.getToDatasetId()));
            list.setToField(bean.getToField());
            list.setRelationType(bean.getRelationType());
            list.setDescription(bean.getDescription());
            list.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bean.getCtime()));
            return list;
        }).collect(Collectors.toList());
    }
}