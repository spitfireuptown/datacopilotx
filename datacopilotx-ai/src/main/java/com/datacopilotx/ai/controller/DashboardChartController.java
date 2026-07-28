package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.domian.bean.DashboardChartBean;
import com.datacopilotx.ai.service.DashboardChartService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard/chart")
public class DashboardChartController {

    @Resource
    private DashboardChartService dashboardChartService;

    @PostMapping("/save")
    public WebResult<DashboardChartBean> save(@RequestBody DashboardChartBean bean) {
        return WebResult.success(dashboardChartService.save(bean));
    }

    @GetMapping("/list")
    public WebResult<List<DashboardChartBean>> list(@RequestParam Long dashboardId) {
        return WebResult.success(dashboardChartService.list(dashboardId));
    }

    @GetMapping("/get/{id}")
    public WebResult<DashboardChartBean> get(@PathVariable Long id) {
        return WebResult.success(dashboardChartService.getById(id));
    }

    @PutMapping("/update")
    public WebResult<Void> update(@RequestBody Map<String, Object> params) {
        DashboardChartBean bean = DashboardChartBean.builder()
                .id(Long.valueOf(params.get("id").toString()))
                .chartName((String) params.get("chartName"))
                .layoutX(params.get("layoutX") != null ? ((Number) params.get("layoutX")).intValue() : null)
                .layoutY(params.get("layoutY") != null ? ((Number) params.get("layoutY")).intValue() : null)
                .layoutW(params.get("layoutW") != null ? ((Number) params.get("layoutW")).intValue() : null)
                .layoutH(params.get("layoutH") != null ? ((Number) params.get("layoutH")).intValue() : null)
                .build();
        dashboardChartService.update(bean);
        return WebResult.success(null);
    }

    @PutMapping("/updateLayout")
    public WebResult<Void> updateLayout(@RequestBody DashboardChartBean bean) {
        DashboardChartBean updateBean = DashboardChartBean.builder()
                .id(bean.getId())
                .layoutX(bean.getLayoutX())
                .layoutY(bean.getLayoutY())
                .layoutW(bean.getLayoutW())
                .layoutH(bean.getLayoutH())
                .build();
        dashboardChartService.update(updateBean);
        return WebResult.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public WebResult<Void> delete(@PathVariable Long id) {
        dashboardChartService.delete(id);
        return WebResult.success(null);
    }
}
