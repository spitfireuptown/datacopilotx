package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.controller.form.DatasetRelationForm;
import com.datacopilotx.ai.domian.bean.DatasetRelationBean;
import com.datacopilotx.ai.domian.vo.DatasetRelationVO;
import com.datacopilotx.ai.mapper.DatasetRelationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatasetRelationService {

    @Autowired
    private DatasetRelationMapper datasetRelationMapper;

    public List<DatasetRelationVO.ListVO> list() {
        return datasetRelationMapper.selectList(new QueryWrapper<>()).stream().map(bean -> {
            DatasetRelationVO.ListVO list = new DatasetRelationVO.ListVO();
            list.setId(bean.getId());
            list.setFromDatasetId(bean.getFromDatasetId());
            list.setFromDatasetName(bean.getFromDatasetName());
            list.setFromField(bean.getFromField());
            list.setToDatasetId(bean.getToDatasetId());
            list.setToDatasetName(bean.getToDatasetName());
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
        bean.setFromDatasetName(createForm.getFromDatasetName());
        bean.setFromField(createForm.getFromField());
        bean.setToDatasetId(createForm.getToDatasetId());
        bean.setToDatasetName(createForm.getToDatasetName());
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
        bean.setFromDatasetName(updateForm.getFromDatasetName());
        bean.setFromField(updateForm.getFromField());
        bean.setToDatasetId(updateForm.getToDatasetId());
        bean.setToDatasetName(updateForm.getToDatasetName());
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

        DatasetRelationVO.DetailVO detail = new DatasetRelationVO.DetailVO();
        detail.setId(bean.getId());
        detail.setFromDatasetId(bean.getFromDatasetId());
        detail.setFromDatasetName(bean.getFromDatasetName());
        detail.setFromField(bean.getFromField());
        detail.setToDatasetId(bean.getToDatasetId());
        detail.setToDatasetName(bean.getToDatasetName());
        detail.setToField(bean.getToField());
        detail.setRelationType(bean.getRelationType());
        detail.setDescription(bean.getDescription());
        detail.setCtime(bean.getCtime());
        detail.setUtime(bean.getUtime());
        return detail;
    }

    public List<DatasetRelationVO.ListVO> listByDatasetId(Long datasetId) {
        return datasetRelationMapper.selectByDatasetId(datasetId).stream().map(bean -> {
            DatasetRelationVO.ListVO list = new DatasetRelationVO.ListVO();
            list.setId(bean.getId());
            list.setFromDatasetId(bean.getFromDatasetId());
            list.setFromDatasetName(bean.getFromDatasetName());
            list.setFromField(bean.getFromField());
            list.setToDatasetId(bean.getToDatasetId());
            list.setToDatasetName(bean.getToDatasetName());
            list.setToField(bean.getToField());
            list.setRelationType(bean.getRelationType());
            list.setDescription(bean.getDescription());
            list.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bean.getCtime()));
            return list;
        }).collect(Collectors.toList());
    }
}