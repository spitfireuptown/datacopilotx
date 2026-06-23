package com.datacopilotx.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacopilotx.ai.domian.bean.UserBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserBean> {

    @Select("SELECT * FROM SYSTEM_USER WHERE username = #{username} AND is_del = 0")
    UserBean findByUsername(@Param("username") String username);

    @Select("SELECT * FROM SYSTEM_USER WHERE user_id = #{userId} AND is_del = 0")
    UserBean findByUserId(@Param("userId") String userId);
}