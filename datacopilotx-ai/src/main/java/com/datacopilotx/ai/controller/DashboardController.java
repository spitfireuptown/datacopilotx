package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.domian.bean.DashboardBean;
import com.datacopilotx.ai.domian.dto.QuestionWithChartDTO;
import com.datacopilotx.ai.service.ChatBusinessService;
import com.datacopilotx.ai.service.DashboardService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private ChatBusinessService chatBusinessService;

    @PostMapping("/create")
    public WebResult<DashboardBean> create(@RequestBody Map<String, String> params) {
        DashboardBean bean = DashboardBean.builder()
                .name(params.get("name"))
                .description(params.getOrDefault("description", ""))
                .build();
        return WebResult.success(dashboardService.create(bean));
    }

    @GetMapping("/list")
    public WebResult<List<DashboardBean>> list() {
        return WebResult.success(dashboardService.list());
    }

    @PutMapping("/rename")
    public WebResult<Void> rename(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String name = (String) params.get("name");
        dashboardService.rename(id, name);
        return WebResult.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public WebResult<Void> delete(@PathVariable Long id) {
        dashboardService.delete(id);
        return WebResult.success(null);
    }

    /** 获取含有图表数据的问数记录列表，用于仪表盘添加图表 */
    @GetMapping("/questions")
    public WebResult<List<QuestionWithChartDTO>> getQuestionsWithChart() {
        return WebResult.success(chatBusinessService.getQuestionsWithChart());
    }
}
