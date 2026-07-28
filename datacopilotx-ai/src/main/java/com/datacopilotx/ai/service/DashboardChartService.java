package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DashboardChartBean;
import com.datacopilotx.ai.mapper.DashboardChartMapper;
import com.datacopilotx.ai.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardChartService {

    @Resource
    private DashboardChartMapper dashboardChartMapper;

    public DashboardChartBean save(DashboardChartBean bean) {
        String userId = SecurityUtil.getCurrentUserId();
        bean.setCreator(userId);
        dashboardChartMapper.insert(bean);
        return bean;
    }

    public List<DashboardChartBean> list(Long dashboardId) {
        String userId = SecurityUtil.getCurrentUserId();
        return dashboardChartMapper.selectList(
                new LambdaQueryWrapper<DashboardChartBean>()
                        .eq(DashboardChartBean::getDashboardId, dashboardId)
                        .eq(DashboardChartBean::getCreator, userId)
                        .orderByAsc(DashboardChartBean::getCtime)
        );
    }

    public DashboardChartBean getById(Long id) {
        return dashboardChartMapper.selectById(id);
    }

    public void update(DashboardChartBean bean) {
        dashboardChartMapper.updateById(bean);
    }

    public void delete(Long id) {
        dashboardChartMapper.deleteById(id);
    }
}
