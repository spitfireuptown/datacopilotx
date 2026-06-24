package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.QuestionLogDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionLogMapper extends BaseMapper<QuestionLogBean> {

    /**
     * 查询聊天历史记录（分页）
     * 该方法自己实现角色权限过滤，不依赖拦截器
     */
    @InterceptorIgnore(dataPermission = "true")
    IPage<QuestionLogDTO> selectQueryLog(IPage<QuestionLogDTO> page, String searchKey, @Param("currentUserId") String currentUserId);
}
