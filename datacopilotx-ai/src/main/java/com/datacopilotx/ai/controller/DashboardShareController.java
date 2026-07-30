package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.domian.bean.DashboardShareBean;
import com.datacopilotx.ai.domian.dto.SharedDashboardDTO;
import com.datacopilotx.ai.service.DashboardShareService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DashboardShareController {

    @Resource
    private DashboardShareService dashboardShareService;

    /** 生成免密分享链接（需登录，仅所有者） */
    @PostMapping("/dashboard/share/create")
    public WebResult<DashboardShareBean> create(@RequestBody Map<String, Object> params) {
        Long dashboardId = Long.valueOf(params.get("dashboardId").toString());
        Integer expireDays = params.get("expireDays") != null
                ? ((Number) params.get("expireDays")).intValue() : null;
        return WebResult.success(dashboardShareService.createShare(dashboardId, expireDays));
    }

    /** 查询当前有效的分享链接（需登录，仅所有者） */
    @GetMapping("/dashboard/share/info")
    public WebResult<DashboardShareBean> info(@RequestParam Long dashboardId) {
        return WebResult.success(dashboardShareService.getActiveShare(dashboardId));
    }

    /** 撤销分享链接（需登录，仅所有者） */
    @DeleteMapping("/dashboard/share/revoke/{dashboardId}")
    public WebResult<Void> revoke(@PathVariable Long dashboardId) {
        dashboardShareService.revokeShare(dashboardId);
        return WebResult.success(null);
    }

    /** 免密访问仪表盘只读视图（公开接口，凭 token 访问） */
    @GetMapping("/public/dashboard/{token}")
    public WebResult<SharedDashboardDTO> viewShared(@PathVariable String token) {
        return WebResult.success(dashboardShareService.getSharedDashboard(token));
    }
}
