package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DashboardBean;
import com.datacopilotx.ai.domian.bean.DashboardChartBean;
import com.datacopilotx.ai.domian.bean.DashboardShareBean;
import com.datacopilotx.ai.domian.dto.SharedDashboardDTO;
import com.datacopilotx.ai.mapper.DashboardChartMapper;
import com.datacopilotx.ai.mapper.DashboardMapper;
import com.datacopilotx.ai.mapper.DashboardShareMapper;
import com.datacopilotx.ai.util.SecurityUtil;
import com.datacopilotx.common.exception.DataCopilotXException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;

@Service
public class DashboardShareService {

    /** token 无效/过期/已撤销统一使用同一提示，避免泄露链接存在性 */
    private static final String INVALID_LINK_MSG = "分享链接无效或已过期";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000L;

    @Resource
    private DashboardShareMapper dashboardShareMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private DashboardChartMapper dashboardChartMapper;

    /**
     * 生成免密分享链接（同一仪表盘只保留一个有效链接，重新生成会作废旧链接）
     */
    public DashboardShareBean createShare(Long dashboardId, Integer expireDays) {
        String userId = checkOwner(dashboardId);
        // 作废该仪表盘的历史分享链接
        dashboardShareMapper.delete(
                new LambdaQueryWrapper<DashboardShareBean>()
                        .eq(DashboardShareBean::getDashboardId, dashboardId)
        );
        int days = (expireDays == null || expireDays <= 0) ? 7 : Math.min(expireDays, 30);
        DashboardShareBean share = DashboardShareBean.builder()
                .dashboardId(dashboardId)
                .token(generateToken())
                .expireTime(new Timestamp(System.currentTimeMillis() + days * DAY_MILLIS))
                .creator(userId)
                .build();
        dashboardShareMapper.insert(share);
        return share;
    }

    /**
     * 查询当前有效的分享链接，无有效链接返回 null
     */
    public DashboardShareBean getActiveShare(Long dashboardId) {
        checkOwner(dashboardId);
        DashboardShareBean share = dashboardShareMapper.selectOne(
                new LambdaQueryWrapper<DashboardShareBean>()
                        .eq(DashboardShareBean::getDashboardId, dashboardId)
                        .orderByDesc(DashboardShareBean::getCtime)
                        .last("LIMIT 1")
        );
        if (share == null || share.getExpireTime().getTime() < System.currentTimeMillis()) {
            return null;
        }
        return share;
    }

    /**
     * 撤销分享链接
     */
    public void revokeShare(Long dashboardId) {
        checkOwner(dashboardId);
        dashboardShareMapper.delete(
                new LambdaQueryWrapper<DashboardShareBean>()
                        .eq(DashboardShareBean::getDashboardId, dashboardId)
        );
    }

    /**
     * 免密访问：按 token 获取仪表盘只读视图（仅暴露渲染所需字段）
     */
    public SharedDashboardDTO getSharedDashboard(String token) {
        if (token == null || token.length() < 32) {
            throw new DataCopilotXException(INVALID_LINK_MSG);
        }
        DashboardShareBean share = dashboardShareMapper.selectOne(
                new LambdaQueryWrapper<DashboardShareBean>()
                        .eq(DashboardShareBean::getToken, token)
        );
        if (share == null || share.getExpireTime().getTime() < System.currentTimeMillis()) {
            throw new DataCopilotXException(INVALID_LINK_MSG);
        }
        DashboardBean dashboard = dashboardMapper.selectById(share.getDashboardId());
        if (dashboard == null) {
            throw new DataCopilotXException(INVALID_LINK_MSG);
        }
        List<DashboardChartBean> charts = dashboardChartMapper.selectList(
                new LambdaQueryWrapper<DashboardChartBean>()
                        .eq(DashboardChartBean::getDashboardId, dashboard.getId())
                        .orderByAsc(DashboardChartBean::getCtime)
        );
        List<SharedDashboardDTO.SharedChartDTO> chartDTOs = charts.stream()
                .map(c -> SharedDashboardDTO.SharedChartDTO.builder()
                        .id(c.getId())
                        .chartName(c.getChartName())
                        .chartType(c.getChartType())
                        .chartData(c.getChartData())
                        .layoutX(c.getLayoutX())
                        .layoutY(c.getLayoutY())
                        .layoutW(c.getLayoutW())
                        .layoutH(c.getLayoutH())
                        .build())
                .toList();
        return SharedDashboardDTO.builder()
                .name(dashboard.getName())
                .charts(chartDTOs)
                .build();
    }

    /**
     * 校验当前用户是仪表盘所有者，返回当前用户ID
     */
    private String checkOwner(Long dashboardId) {
        String userId = SecurityUtil.getCurrentUserId();
        DashboardBean dashboard = dashboardMapper.selectById(dashboardId);
        if (dashboard == null || userId == null || !userId.equals(dashboard.getCreator())) {
            throw new DataCopilotXException("无权操作该仪表盘");
        }
        return userId;
    }

    /**
     * 生成高熵随机 token：256-bit SecureRandom + Base64URL（43字符，不含业务含义）
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
