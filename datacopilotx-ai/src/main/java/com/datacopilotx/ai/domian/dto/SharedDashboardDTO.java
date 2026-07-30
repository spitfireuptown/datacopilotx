package com.datacopilotx.ai.domian.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 免密分享的仪表盘视图（最小数据暴露：不含 SQL、提问、创建人等敏感信息）
 */
@Data
@Builder
public class SharedDashboardDTO {

    private String name;

    private List<SharedChartDTO> charts;

    @Data
    @Builder
    public static class SharedChartDTO {
        private Long id;
        private String chartName;
        private String chartType;
        private String chartData;
        private Integer layoutX;
        private Integer layoutY;
        private Integer layoutW;
        private Integer layoutH;
    }
}
