package com.datacopilotx.ai.domian.dto;

import cn.hutool.json.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryDTO {

    private List<Column> columns;
    private JSONArray data;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class Column {
        private String type;
        private String name;
    }
}
