package com.datacopilotx.ai.config.interceptor;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.datacopilotx.ai.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据权限拦截器
 * 
 * 优雅地实现数据权限控制，Service层完全无感知
 * 
 * - 超级管理员(role=0)：可以查看所有数据
 * - 管理员(role=1)：可以查看所有数据
 * - 普通用户(role=2)：只能查看自己创建的数据
 * 
 * 自动拦截以下表的查询：
 * - data_set
 * - data_set_relation
 * - knowledge_lib
 * - model_config
 */
@Slf4j
public class DataPermissionInterceptor implements InnerInterceptor {

    /**
     * 需要进行数据权限控制的表名（不区分大小写）
     */
    private static final Set<String> PROTECTED_TABLES = Set.of(
            "data_set",
            "data_set_relation", 
            "knowledge_lib",
            "question_log",
            "model_config"
    );

    /**
     * 超级管理员角色值
     */
    private static final int ROLE_SUPER_ADMIN = 0;

    /**
     * 管理员角色值
     */
    private static final int ROLE_ADMIN = 1;

    /**
     * 用于匹配表名的正则表达式（支持多种格式）
     * 匹配模式：
     * - FROM `table_name`
     * - FROM table_name
     * - FROM schema.table_name
     */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "FROM\\s+(?:`([^`]+)`|([^\\s`,]+))",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, 
                          RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        
        // 获取SQL ID用于日志和忽略配置
        String sqlId = ms.getId();
        
        // 1. 检查是否配置了忽略拦截
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(sqlId)) {
            log.debug("数据权限拦截器: SQL [{}] 已配置忽略", sqlId);
            return;
        }

        // 2. 获取当前用户信息
        String userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            log.debug("数据权限拦截器: 用户未登录，放行所有数据");
            return;
        }

        // 3. 获取用户角色并判断权限
        Integer role = SecurityUtil.getCurrentUserRole();
        if (role == ROLE_SUPER_ADMIN || role == ROLE_ADMIN) {
            log.debug("数据权限拦截器: 用户[{}]角色[{}]为管理员，放行所有数据", userId, role);
            return;
        }

        // 4. 普通用户需要添加数据权限过滤
        applyDataPermission(boundSql, userId, sqlId);
    }

    /**
     * 应用数据权限过滤
     */
    private void applyDataPermission(BoundSql boundSql, String userId, String sqlId) {
        String originalSql = boundSql.getSql();
        
        // 提取表名
        String tableName = extractTableName(originalSql);
        if (tableName == null) {
            log.debug("数据权限拦截器: SQL [{}] 未能提取表名，放行", sqlId);
            return;
        }

        // 检查是否需要权限控制
        String lowerTableName = tableName.toLowerCase();
        if (!PROTECTED_TABLES.contains(lowerTableName)) {
            log.debug("数据权限拦截器: SQL [{}] 表[{}]不在保护列表中，放行", sqlId, tableName);
            return;
        }

        // 添加creator过滤条件
        String securedSql = injectCreatorCondition(originalSql, userId);
        log.info("数据权限拦截器: SQL [{}] 表[{}]应用权限过滤，用户[{}]", sqlId, tableName, userId);
        
        // 修改BoundSql中的SQL
        PluginUtils.mpBoundSql(boundSql).sql(securedSql);
    }

    /**
     * 从SQL语句中提取表名
     */
    private String extractTableName(String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        Matcher matcher = TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            // 返回第一个非空的分组
            String group1 = matcher.group(1);
            String group2 = matcher.group(2);
            String tableName = group1 != null ? group1 : group2;
            
            // 如果表名包含点号（如 schema.table），只取表名部分
            if (tableName.contains(".")) {
                tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
            }
            
            return tableName;
        }
        
        return null;
    }

    /**
     * 注入creator过滤条件
     */
    private String injectCreatorCondition(String sql, String userId) {
        // 查找WHERE关键字的位置（带空格，避免匹配WHEREABOUTS等词）
        int whereIndex = sql.toLowerCase().indexOf(" where ");
        
        // 处理分页SQL的情况（如: SELECT COUNT(*) FROM (SELECT * FROM table WHERE ...) TOTAL）
        int fromParenIndex = sql.toLowerCase().indexOf("from (");
        if (fromParenIndex > 0) {
            // 找到子查询的结束括号位置（跳过函数内部的括号）
            int closeParenIndex = findSubqueryClosingParen(sql, fromParenIndex + 5);
            if (closeParenIndex > 0) {
                // 在子查询内部查找WHERE位置
                String innerSql = sql.substring(fromParenIndex + 5, closeParenIndex);
                int innerWhereIndex = innerSql.toLowerCase().indexOf(" where ");
                
                if (innerWhereIndex > 0) {
                    // 在子查询内部已有WHERE条件，查找ORDER BY等关键字的位置
                    int innerOrderByIndex = findKeywordPosition(innerSql, " order by ");
                    int innerGroupByIndex = findKeywordPosition(innerSql, " group by ");
                    int innerLimitIndex = findKeywordPosition(innerSql, " limit ");
                    
                    int endOfWhereConditions = findEndOfWhereConditions(innerSql, innerWhereIndex + 7);
                    
                    return sql.substring(0, fromParenIndex + 5 + endOfWhereConditions)
                            + " AND creator = '" + userId + "'"
                            + sql.substring(fromParenIndex + 5 + innerOrderByIndex);
                } else {
                    // 在子查询内部没有WHERE条件，找到GROUP BY、ORDER BY或LIMIT之前的位置插入
                    int insertIndex = findInsertPositionInQuery(innerSql);
                    int actualInsertIndex = fromParenIndex + 5 + insertIndex;
                    
                    return sql.substring(0, actualInsertIndex)
                            + " WHERE creator = '" + userId + "' "
                            + sql.substring(actualInsertIndex);
                }
            }
        }
        
        // 处理普通SQL的情况
        if (whereIndex > 0) {
            // 如果已有WHERE条件，添加AND条件
            // 需要在ORDER BY、GROUP BY、LIMIT之前插入条件
            int orderByIndex = findKeywordPosition(sql, " order by ");
            int groupByIndex = findKeywordPosition(sql, " group by ");
            int limitIndex = findKeywordPosition(sql, " limit ");
            
            // 找到WHERE条件结束的位置（即ORDER BY、GROUP BY或LIMIT之前）
            int endOfWhereConditions = findEndOfWhereConditions(sql, whereIndex + 7);
            
            // 在WHERE条件后、ORDER BY等关键字前插入AND条件
            return sql.substring(0, endOfWhereConditions)
                    + " AND creator = '" + userId + "'"
                    + sql.substring(endOfWhereConditions);
        } else {
            // 如果没有WHERE条件，找到GROUP BY、ORDER BY或LIMIT之前的位置插入
            int insertIndex = findInsertPositionInQuery(sql);
            return sql.substring(0, insertIndex) + " WHERE creator = '" + userId + "' " + sql.substring(insertIndex);
        }
    }
    
    /**
     * 查找关键字在SQL中的位置
     */
    private int findKeywordPosition(String sql, String keyword) {
        int index = sql.toLowerCase().indexOf(keyword);
        return index > 0 ? index : sql.length();
    }
    
    /**
     * 找到WHERE条件结束的位置（即ORDER BY、GROUP BY、LIMIT等关键字之前）
     */
    private int findEndOfWhereConditions(String sql, int startPos) {
        String lowerSql = sql.toLowerCase();
        
        int orderByIndex = lowerSql.indexOf(" order by ", startPos);
        int groupByIndex = lowerSql.indexOf(" group by ", startPos);
        int limitIndex = lowerSql.indexOf(" limit ", startPos);
        
        // 找到最近的关键词位置
        int minIndex = sql.length();
        
        if (orderByIndex > 0 && orderByIndex < minIndex) {
            minIndex = orderByIndex;
        }
        if (groupByIndex > 0 && groupByIndex < minIndex) {
            minIndex = groupByIndex;
        }
        if (limitIndex > 0 && limitIndex < minIndex) {
            minIndex = limitIndex;
        }
        
        return minIndex;
    }
    
    /**
     * 查找子查询的结束括号位置（跳过函数调用内部的括号）
     */
    private int findSubqueryClosingParen(String sql, int startIndex) {
        int depth = 1;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (int i = startIndex; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            // 处理引号
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            
            // 跳过引号内的内容
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 查找条件插入位置（在 GROUP BY、ORDER BY、LIMIT、UNION 之前）
     */
    private int findInsertPositionInQuery(String sql) {
        String lowerSql = sql.toLowerCase();
        
        int groupByIndex = lowerSql.indexOf(" group by ");
        int orderByIndex = lowerSql.indexOf(" order by ");
        int limitIndex = lowerSql.indexOf(" limit ");
        int unionIndex = lowerSql.indexOf(" union ");
        
        int insertIndex = sql.length();
        
        if (groupByIndex > 0 && groupByIndex < insertIndex) {
            insertIndex = groupByIndex;
        }
        if (orderByIndex > 0 && orderByIndex < insertIndex) {
            insertIndex = orderByIndex;
        }
        if (limitIndex > 0 && limitIndex < insertIndex) {
            insertIndex = limitIndex;
        }
        if (unionIndex > 0 && unionIndex < insertIndex) {
            insertIndex = unionIndex;
        }
        
        return insertIndex;
    }
}