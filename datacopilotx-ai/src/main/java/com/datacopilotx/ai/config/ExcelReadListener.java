package com.datacopilotx.ai.config;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: uptown
 * @date: 2025/11/22 14:08
 */
@Slf4j
public class ExcelReadListener extends AnalysisEventListener<Map<Integer, Object>> {
    private Map<Integer, Object> headMap = new HashMap<>();
    private List<Map<Integer, Object>> contentList = new ArrayList<>();
    private String dept;

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        // 正确处理表头数据
        Map<Integer, Object> convertedHeadMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            convertedHeadMap.put(entry.getKey(), entry.getValue());
        }
        this.headMap = convertedHeadMap;
        log.info("表头数据: {}", JSONUtil.parse(convertedHeadMap));
    }

    @Override
    public void invoke(Map<Integer, Object> contentMap, AnalysisContext context) {
        ReadRowHolder readRowHolder = context.readRowHolder();
        Integer rowIndex = readRowHolder.getRowIndex();

        // 不再将第一行数据当作表头处理，直接处理内容数据
        for (Map.Entry<Integer, Object> entry : contentMap.entrySet()) {
            Integer key = entry.getKey();
            Object value = entry.getValue();

            if (key == 0) {
                if (value == null) {
                    contentMap.replace(key, dept);
                } else {
                    dept = String.valueOf(value);
                }
            }
        }
        log.info("<{}> content: {} ", rowIndex, JSONUtil.parse(contentMap));
        this.contentList.add(contentMap);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        ReadSheetHolder readSheetHolder = context.readSheetHolder();
        String sheetName = readSheetHolder.getSheetName();
        log.info("<{}> read successfully! total: {}", sheetName, this.contentList.size());
    }

    public Map<Integer, Object> getHeadMap() {
        return headMap;
    }

    public List<Map<Integer, Object>> getContentList() {
        return contentList;
    }
}
