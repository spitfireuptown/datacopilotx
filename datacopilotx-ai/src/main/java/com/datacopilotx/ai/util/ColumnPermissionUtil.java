package com.datacopilotx.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 列权限处理工具类
 * 用于改写SQL，隐藏不允许访问的字段
 */
@Slf4j
public class ColumnPermissionUtil {

    private static final Pattern SELECT_PATTERN = Pattern.compile("select\\s+(.+?)\\s+from", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FIELD_PATTERN = Pattern.compile("(\\w+\\s*)(?:as\\s+\\w+)?", Pattern.CASE_INSENSITIVE);

    /**
     * 改写SQL，移除不允许访问的字段
     * @param sql 原始SQL
     * @param allowedFields 允许访问的字段列表
     * @param allFields SQL中所有字段列表
     * @return 改写后的SQL，如果所有字段都被隐藏则返回null
     */
    public static String rewriteSqlForColumnPermission(String sql, List<String> allowedFields, List<String> allFields) {
        if (allowedFields == null || allowedFields.isEmpty()) {
            log.warn("没有允许访问的字段，返回null");
            return null;
        }

        if (allowedFields.size() == allFields.size() && new HashSet<>(allowedFields).containsAll(allFields)) {
            return sql;
        }

        try {
            Matcher selectMatcher = SELECT_PATTERN.matcher(sql);
            if (!selectMatcher.find()) {
                log.warn("SQL格式不正确，无法解析SELECT部分");
                return sql;
            }

            String selectPart = selectMatcher.group(1);
            String fromPart = sql.substring(selectMatcher.end());

            Set<String> allowedSet = new HashSet<>(allowedFields);

            List<String> newFields = new ArrayList<>();
            String[] fields = selectPart.split(",");
            for (String field : fields) {
                field = field.trim();
                String fieldName = extractFieldName(field);
                if (fieldName != null && allowedSet.contains(fieldName)) {
                    newFields.add(field);
                }
            }

            if (newFields.isEmpty()) {
                log.warn("所有字段都被隐藏，返回null");
                return null;
            }

            String newSelectPart = String.join(", ", newFields);
            return "SELECT " + newSelectPart + " FROM" + fromPart;

        } catch (Exception e) {
            log.error("改写SQL失败: {}", e.getMessage());
            return sql;
        }
    }

    /**
     * 从字段表达式中提取字段名
     * @param fieldExpression 字段表达式（可能包含AS别名）
     * @return 字段名
     */
    private static String extractFieldName(String fieldExpression) {
        fieldExpression = fieldExpression.trim();

        if (fieldExpression.contains("(")) {
            Pattern funcPattern = Pattern.compile("\\w+\\s*\\((.*?)\\)", Pattern.CASE_INSENSITIVE);
            Matcher funcMatcher = funcPattern.matcher(fieldExpression);
            if (funcMatcher.find()) {
                String inner = funcMatcher.group(1).trim();
                if (!inner.contains(",")) {
                    return extractFieldName(inner);
                }
            }
            return fieldExpression.split("\\(")[0].trim();
        }

        String[] parts = fieldExpression.split("\\s+as\\s+", Pattern.CASE_INSENSITIVE);
        if (parts.length >= 1) {
            String fieldPart = parts[0].trim();
            if (fieldPart.startsWith("`") && fieldPart.endsWith("`")) {
                return fieldPart.substring(1, fieldPart.length() - 1);
            }
            if (fieldPart.startsWith("\"") && fieldPart.endsWith("\"")) {
                return fieldPart.substring(1, fieldPart.length() - 1);
            }
            if (fieldPart.startsWith("'") && fieldPart.endsWith("'")) {
                return fieldPart.substring(1, fieldPart.length() - 1);
            }
            return fieldPart;
        }

        return fieldExpression;
    }

    /**
     * 从SQL中提取所有字段名
     * @param sql SQL语句
     * @return 字段名列表
     */
    public static List<String> extractFieldsFromSql(String sql) {
        List<String> fields = new ArrayList<>();

        try {
            Matcher selectMatcher = SELECT_PATTERN.matcher(sql);
            if (!selectMatcher.find()) {
                return fields;
            }

            String selectPart = selectMatcher.group(1);
            String[] fieldExpressions = selectPart.split(",");

            for (String expression : fieldExpressions) {
                expression = expression.trim();
                String fieldName = extractFieldName(expression);
                if (fieldName != null && !fieldName.isEmpty()) {
                    fields.add(fieldName);
                }
            }

        } catch (Exception e) {
            log.error("从SQL提取字段失败: {}", e.getMessage());
        }

        return fields;
    }

    /**
     * 判断SQL是否包含聚合函数
     * @param sql SQL语句
     * @return true表示包含聚合函数
     */
    public static boolean hasAggregateFunction(String sql) {
        String lowerSql = sql.toLowerCase();
        return lowerSql.contains("count(") ||
               lowerSql.contains("sum(") ||
               lowerSql.contains("avg(") ||
               lowerSql.contains("max(") ||
               lowerSql.contains("min(");
    }

    /**
     * 处理聚合函数查询的列权限
     * 对于聚合函数查询，保留聚合函数，但替换内部字段
     * @param sql 原始SQL
     * @param allowedFields 允许访问的字段列表
     * @return 改写后的SQL
     */
    public static String rewriteAggregateSql(String sql, List<String> allowedFields) {
        if (!hasAggregateFunction(sql)) {
            return rewriteSqlForColumnPermission(sql, allowedFields, extractFieldsFromSql(sql));
        }

        Set<String> allowedSet = new HashSet<>(allowedFields);

        try {
            Matcher selectMatcher = SELECT_PATTERN.matcher(sql);
            if (!selectMatcher.find()) {
                return sql;
            }

            String selectPart = selectMatcher.group(1);
            String fromPart = sql.substring(selectMatcher.end());

            List<String> newFields = new ArrayList<>();
            String[] fields = selectPart.split(",");

            for (String field : fields) {
                field = field.trim();
                if (field.toLowerCase().matches("^\\w+\\s*\\(.*\\)$")) {
                    Pattern funcPattern = Pattern.compile("(\\w+)\\s*\\((.*?)\\)", Pattern.CASE_INSENSITIVE);
                    Matcher funcMatcher = funcPattern.matcher(field);
                    if (funcMatcher.matches()) {
                        String funcName = funcMatcher.group(1);
                        String innerField = funcMatcher.group(2).trim();
                        String innerFieldName = extractFieldName(innerField);

                        if (innerFieldName.equals("*") || allowedSet.contains(innerFieldName)) {
                            newFields.add(field);
                        } else {
                            newFields.add(funcName + "(1)");
                        }
                    } else {
                        newFields.add(field);
                    }
                } else {
                    String fieldName = extractFieldName(field);
                    if (allowedSet.contains(fieldName)) {
                        newFields.add(field);
                    }
                }
            }

            if (newFields.isEmpty()) {
                return null;
            }

            String newSelectPart = String.join(", ", newFields);
            return "SELECT " + newSelectPart + " FROM" + fromPart;

        } catch (Exception e) {
            log.error("改写聚合SQL失败: {}", e.getMessage());
            return sql;
        }
    }
}