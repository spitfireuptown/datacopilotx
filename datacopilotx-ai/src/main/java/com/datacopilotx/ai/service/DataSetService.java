package com.datacopilotx.ai.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.controller.form.DataSetForm;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.vo.DataSetVO;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.DataSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataSetService {

    @Autowired
    DataSetMapper dataSetMapper;
    @Autowired
    QuestionLogMapper questionLogMapper;


    public List<DataSetVO.ListVO> list() {
        return dataSetMapper.selectList(new QueryWrapper<>()).stream().map(dataSetBean -> {
            DataSetVO.ListVO list = new DataSetVO.ListVO();
            list.setId(dataSetBean.getId());
            list.setTable(dataSetBean.getDatabase() + "." + dataSetBean.getTable());
            list.setName(dataSetBean.getDsName());
            list.setType(dataSetBean.getType());
            list.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dataSetBean.getCtime()));
            return list;
        }).collect(Collectors.toList());
    }

    public Long create(DataSetForm.Create createForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setDsName(createForm.getName());
        dataSetBean.setDatabase(createForm.getDatabase());
        dataSetBean.setHost(createForm.getHost());
        dataSetBean.setPort(createForm.getPort());
        dataSetBean.setTable(createForm.getTable());
        dataSetBean.setUsername(createForm.getUsername());
        dataSetBean.setPassword(createForm.getPassword());
        dataSetBean.setFields(JSONUtil.toJsonStr(createForm.getFields()));
        dataSetBean.setType(createForm.getType());
        dataSetBean.setDescription(createForm.getDescription());
        dataSetBean.setInjectPrompt(createForm.getPrompt());

        dataSetMapper.insert(dataSetBean);
        return dataSetBean.getId();
    }


    public Long update(DataSetForm.Create updateForm) {
        DataSetBean dataSetBean = new DataSetBean();
        dataSetBean.setDsName(updateForm.getName());
        dataSetBean.setDatabase(updateForm.getDatabase());
        dataSetBean.setHost(updateForm.getHost());
        dataSetBean.setPort(updateForm.getPort());
        dataSetBean.setTable(updateForm.getTable());
        dataSetBean.setUsername(updateForm.getUsername());
        dataSetBean.setPassword(updateForm.getPassword());
        dataSetBean.setFields(JSONUtil.toJsonStr(updateForm.getFields()));
        dataSetBean.setType(updateForm.getType());
        dataSetBean.setInjectPrompt(updateForm.getPrompt());
        dataSetBean.setDescription(updateForm.getDescription());

        dataSetMapper.update(dataSetBean, new LambdaQueryWrapper<DataSetBean>().eq(DataSetBean::getId, updateForm.getId()));
        return updateForm.getId();
    }


    public List<DataSetDTO.SchemaInfo> tableSchemaInfo(DataSetForm.Create createForm) {
        List<DataSetDTO.SchemaInfo> result;
        try {
            DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                    .builder()
                    .host(createForm.getHost())
                    .port(createForm.getPort())
                    .database(createForm.getDatabase())
                    .table(createForm.getTable())
                    .username(createForm.getUsername())
                    .password(createForm.getPassword())
                    .type(createForm.getType())
                    .build();
            JDBCDriver driver = DriverFactory.getDriver(driverInfo);
            result = driver.fetchColumn(driverInfo);
        } catch (Exception e) {
            throw new DataCopilotXException(e.getMessage());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void del(long id) {
        DataSetBean dataSetBean = dataSetMapper.selectById(id);
        if (dataSetBean == null) {
            throw new DataCopilotXException("删除数据集不存在");
        }
        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                .builder()
                .host(dataSetBean.getHost())
                .port(dataSetBean.getPort())
                .database(dataSetBean.getDatabase())
                .table(dataSetBean.getTable())
                .username(dataSetBean.getUsername())
                .password(dataSetBean.getPassword())
                .type(dataSetBean.getType())
                .build();
        DriverFactory.removeDriver(driverInfo);
        dataSetMapper.deleteById(id);
        questionLogMapper.delete(new LambdaQueryWrapper<QuestionLogBean>().eq(QuestionLogBean::getDataId, id));
    }

    public DataSetVO.DetailVO detail(long id) {
        DataSetBean dataSetBean = dataSetMapper.selectOne(new LambdaQueryWrapper<DataSetBean>().eq(DataSetBean::getId, id));
        DataSetVO.DetailVO detailVO = new DataSetVO.DetailVO();
        detailVO.setId(dataSetBean.getId());
        detailVO.setName(dataSetBean.getDsName());
        detailVO.setType(dataSetBean.getType());
        detailVO.setHost(dataSetBean.getHost());
        detailVO.setPort(dataSetBean.getPort());
        detailVO.setDatabase(dataSetBean.getDatabase());
        detailVO.setTable(dataSetBean.getTable());
        detailVO.setUsername(dataSetBean.getUsername());
        detailVO.setPassword(dataSetBean.getPassword());
        detailVO.setDescription(dataSetBean.getDescription());
        detailVO.setPrompt(dataSetBean.getInjectPrompt());
        detailVO.setFields(JSONUtil.toList(dataSetBean.getFields(), DataSetDTO.SchemaInfo.class));
        return detailVO;
    }
}
