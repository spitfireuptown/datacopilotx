package com.datacopilotx.ai.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.datacopilotx.ai.config.interceptor.DataPermissionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加数据权限拦截器（需要放在分页拦截器之前）
        interceptor.addInnerInterceptor(dataPermissionInterceptor());
        
        // 添加分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.MYSQL);
        paginationInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }

    /**
     * 数据权限拦截器
     * - 超级管理员(role=0)：可以查看所有数据
     * - 管理员(role=1)：可以查看所有数据
     * - 普通用户(role=2)：只能查看自己创建的数据
     */
    @Bean
    public DataPermissionInterceptor dataPermissionInterceptor() {
        return new DataPermissionInterceptor();
    }
}
