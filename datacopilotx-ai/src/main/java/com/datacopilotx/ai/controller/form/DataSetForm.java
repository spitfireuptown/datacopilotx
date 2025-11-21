package com.datacopilotx.ai.controller.form;

import com.datacopilotx.ai.domian.dto.DataSetDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author: uptown
 * @date: 2025/8/31 12:09
 */
@Data
public class DataSetForm {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class Create {
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
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class FileUploadForm {
        private MultipartFile file;
        private String name;
        private String description;
    }
}
