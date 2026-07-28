package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DashboardBean;
import com.datacopilotx.ai.mapper.DashboardMapper;
import com.datacopilotx.ai.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    @Resource
    private DashboardMapper dashboardMapper;

    public DashboardBean create(DashboardBean bean) {
        bean.setCreator(SecurityUtil.getCurrentUserId());
        dashboardMapper.insert(bean);
        return bean;
    }

    public List<DashboardBean> list() {
        return dashboardMapper.selectList(
                new LambdaQueryWrapper<DashboardBean>()
                        .eq(DashboardBean::getCreator, SecurityUtil.getCurrentUserId())
                        .orderByAsc(DashboardBean::getCtime)
        );
    }

    public void rename(Long id, String name) {
        DashboardBean bean = DashboardBean.builder().id(id).name(name).build();
        dashboardMapper.updateById(bean);
    }

    public void delete(Long id) {
        dashboardMapper.deleteById(id);
    }
}
