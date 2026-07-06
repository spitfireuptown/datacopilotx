package com.datacopilotx.ai.domian.vo;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

public class DataSetVO {

    @Data
    public static final class ListVO {
       private Long id;
       private String meta;
       private String name;
       private String table;
       private String type;
       private String createTime;
       private String creator;
       private String creatorName;
    }

    @Data
    public static final class DetailVO {
        private Long id;
        private String name;
        private String type;
        private String host;
        private Long port;
        private String database;
        private String username;
        private String password;
        private String description;
        private List<TableVO> tables;
    }

    @Data
    public static final class TableVO {
        private Long id;
        private String table;
        private String prompt;
        private List<DataSetDTO.SchemaInfo> fields;
    }
}
