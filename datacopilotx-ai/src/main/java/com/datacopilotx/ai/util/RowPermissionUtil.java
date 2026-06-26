package com.datacopilotx.ai.util;

import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.dto.PermissionDTO;
import com.datacopilotx.ai.domian.dto.PermissionDTO.ExpressionItem;
import com.datacopilotx.ai.domian.dto.PermissionDTO.ExpressionTree;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RowPermissionUtil {

    private static final Set<String> LOGIC_OPERATORS = Set.of("and", "or");

    private static final Map<String, String> TERM_MAP = Map.ofEntries(
            Map.entry("eq", "="),
            Map.entry("not_eq", "<>"),
            Map.entry("lt", "<"),
            Map.entry("le", "<="),
            Map.entry("gt", ">"),
            Map.entry("ge", ">="),
            Map.entry("in", "IN"),
            Map.entry("not in", "NOT IN"),
            Map.entry("like", "LIKE"),
            Map.entry("not like", "NOT LIKE"),
            Map.entry("null", "IS NULL"),
            Map.entry("not_null", "IS NOT NULL"),
            Map.entry("empty", "= ''"),
            Map.entry("not_empty", "<> ''"),
            Map.entry("between", "BETWEEN")
    );

    private static final Map<String, String> SYSTEM_VARS = Map.of(
            "name", "username",
            "account", "user_id",
            "email", "email"
    );

    public static String convertExpressionTreeToSql(String expressionTreeJson, String currentUserId, Map<String, Object> userInfo) {
        if (expressionTreeJson == null || expressionTreeJson.isEmpty()) {
            return "";
        }

        try {
            ExpressionTree tree = JSONUtil.toBean(expressionTreeJson, ExpressionTree.class);
            return transTreeToSql(tree, currentUserId, userInfo);
        } catch (Exception e) {
            log.error("转换表达式树失败: {}", e.getMessage());
            return "";
        }
    }

    private static String transTreeToSql(ExpressionTree tree, String currentUserId, Map<String, Object> userInfo) {
        if (tree == null || tree.getItems() == null || tree.getItems().isEmpty()) {
            return "";
        }

        List<String> conditions = tree.getItems().stream()
                .map(item -> transItemToSql(item, currentUserId, userInfo))
                .filter(s -> !s.isEmpty())
                .toList();

        if (conditions.isEmpty()) {
            return "";
        }

        String logic = tree.getLogic() != null ? tree.getLogic().toLowerCase() : "and";
        if (!LOGIC_OPERATORS.contains(logic)) {
            logic = "and";
        }

        return "(" + String.join(" " + logic.toUpperCase() + " ", conditions) + ")";
    }

    private static String transItemToSql(ExpressionItem item, String currentUserId, Map<String, Object> userInfo) {
        if (item == null) {
            return "";
        }

        if ("tree".equals(item.getType())) {
            return transTreeToSql(item.getSubTree(), currentUserId, userInfo);
        }

        if ("item".equals(item.getType())) {
            return buildCondition(item, currentUserId, userInfo);
        }

        return "";
    }

    private static String buildCondition(ExpressionItem item, String currentUserId, Map<String, Object> userInfo) {
        String fieldName = item.getFieldName();
        String term = item.getTerm();
        Object value = item.getValue();

        if (fieldName == null || term == null) {
            return "";
        }

        String operator = TERM_MAP.get(term.toLowerCase());
        if (operator == null) {
            log.warn("未知操作符: {}", term);
            return "";
        }

        if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
            return "`" + fieldName + "` " + operator;
        }

        if ("= ''".equals(operator) || "<> ''".equals(operator)) {
            return "`" + fieldName + "` " + operator;
        }

        value = resolveValue(value, currentUserId, userInfo);

        if ("IN".equals(operator) || "NOT IN".equals(operator)) {
            if (value instanceof List<?> list) {
                String values = list.stream()
                        .map(v -> formatValue(v))
                        .toList()
                        .toString()
                        .replace("[", "(")
                        .replace("]", ")");
                return "`" + fieldName + "` " + operator + " " + values;
            }
            return "";
        }

        if ("BETWEEN".equals(operator)) {
            if (value instanceof List<?> list && list.size() == 2) {
                return "`" + fieldName + "` " + operator + " " + formatValue(list.get(0)) + " AND " + formatValue(list.get(1));
            }
            return "";
        }

        if ("LIKE".equals(operator) || "NOT LIKE".equals(operator)) {
            if (value instanceof String str) {
                if (!str.contains("%")) {
                    value = "%" + str + "%";
                }
            }
        }

        return "`" + fieldName + "` " + operator + " " + formatValue(value);
    }

    private static Object resolveValue(Object value, String currentUserId, Map<String, Object> userInfo) {
        if (value instanceof String str) {
            if (str.startsWith("$") && str.endsWith("$")) {
                String varName = str.substring(1, str.length() - 1);
                if ("user_id".equals(varName) || "userId".equals(varName)) {
                    return currentUserId;
                }
                if (userInfo != null && userInfo.containsKey(varName)) {
                    return userInfo.get(varName);
                }
                if (SYSTEM_VARS.containsKey(varName) && userInfo != null) {
                    return userInfo.get(SYSTEM_VARS.get(varName));
                }
            }
        }
        return value;
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    public static boolean isValidExpressionTree(String expressionTreeJson) {
        if (expressionTreeJson == null || expressionTreeJson.isEmpty()) {
            return false;
        }
        try {
            ExpressionTree tree = JSONUtil.toBean(expressionTreeJson, ExpressionTree.class);
            return validateTree(tree);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateTree(ExpressionTree tree) {
        if (tree == null) {
            return false;
        }
        if (tree.getLogic() == null || !LOGIC_OPERATORS.contains(tree.getLogic().toLowerCase())) {
            return false;
        }
        if (tree.getItems() == null || tree.getItems().isEmpty()) {
            return false;
        }
        return tree.getItems().stream().allMatch(RowPermissionUtil::validateItem);
    }

    private static boolean validateItem(ExpressionItem item) {
        if (item == null) {
            return false;
        }
        if ("tree".equals(item.getType())) {
            return validateTree(item.getSubTree());
        }
        if ("item".equals(item.getType())) {
            return item.getFieldName() != null && item.getTerm() != null && TERM_MAP.containsKey(item.getTerm().toLowerCase());
        }
        return false;
    }
}