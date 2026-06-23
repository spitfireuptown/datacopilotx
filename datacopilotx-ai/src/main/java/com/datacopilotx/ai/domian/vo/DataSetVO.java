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
    }

    @Data
    public static final class DetailVO {
        private Long id;
        private String name;
        private String type;
        private String host;
        private Long port;
        private String database;
        private String table;
        private String username;
        private String password;
        private String description;
        private String prompt;
        private List<DataSetDTO.SchemaInfo> fields;
        private List<DataSetDTO.TableRelation> relations;
    }
}
