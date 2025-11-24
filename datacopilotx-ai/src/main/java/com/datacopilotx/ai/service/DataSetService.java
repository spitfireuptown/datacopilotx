package com.datacopilotx.ai.service;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacopilotx.ai.config.ExcelReadListener;
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
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.agent.tool.P;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataSetService {

    @Autowired
    DataSetMapper dataSetMapper;
    @Autowired
    QuestionLogMapper questionLogMapper;
    @Autowired
    private HikariDataSource hikariDataSource;


    public List<DataSetVO.ListVO> list() {
        return dataSetMapper.selectList(new QueryWrapper<>()).stream().map(dataSetBean -> {
            DataSetVO.ListVO list = new DataSetVO.ListVO();
            list.setId(dataSetBean.getId());
            if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
                list.setTable(dataSetBean.getTable());
            } else {
                list.setTable(dataSetBean.getDatabase() + "." + dataSetBean.getTable());
            }
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

    public List<DataSetDTO.SchemaInfo> fileUpload(MultipartFile file, String name, String description) {
        List<DataSetDTO.SchemaInfo> result = new ArrayList<>();

        ExcelReadListener readListener = new ExcelReadListener();
        ExcelReader excelReader = null;
        List<ReadSheet> readSheetList = null;
        
        // 使用try-with-resources确保资源正确关闭
        try (InputStream inputStream = file.getInputStream()) {
            ExcelReaderBuilder readerBuilder = EasyExcel.read(inputStream, readListener);
            excelReader = readerBuilder.build();
            readSheetList = excelReader.excelExecutor().sheetList();

            // 只处理第一个工作表的表头
            if (!readSheetList.isEmpty()) {
                ReadSheet firstSheet = readSheetList.get(0);
                excelReader.read(firstSheet);
                Map<Integer, Object> headMap = readListener.getHeadMap();
                
                // 将表头转换为SchemaInfo对象
                for (Map.Entry<Integer, Object> entry : headMap.entrySet()) {
                    Object header = entry.getValue();
                    if (header != null) {
                        DataSetDTO.SchemaInfo schemaInfo = new DataSetDTO.SchemaInfo();
                        schemaInfo.setFieldName(header.toString());
                        schemaInfo.setFieldType("String"); // 默认设置为String类型
                        result.add(schemaInfo);
                    }
                }
                log.info("<{}> 表头处理完成，共 {} 个字段", firstSheet.getSheetName(), result.size());
            }
        } catch (IOException e) {
            log.error("读取Excel文件时发生错误: ", e);
            throw new DataCopilotXException("读取Excel文件失败");
        }
        
        return result;
    }
}
