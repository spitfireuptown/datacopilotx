package com.datacopilotx.common.util;

import com.datacopilotx.common.constant.GlobalConstant;

import java.util.ArrayList;
import java.util.List;

public class WorkflowUtil {

    public static String cleanJsonStr(String data) {
        int startIndex = data.indexOf("```json");
        int endIndex = data.lastIndexOf("```");

        if (startIndex == -1 || endIndex == -1) {
            return data;
        }

        return data.substring(startIndex + "```json".length(), endIndex).replace("\n", "");
    }

    public static String innerIndexName(String indexName) {
        return "%s_%s".formatted(GlobalConstant.COMMON_INDEX_NAME, indexName);
    }

    // 切割字符串为指定长度的子字符串
    public static List<String> splitString(String str, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (str == null || str.isEmpty() || chunkSize <= 0) {
            return chunks;
        }
        for (int i = 0; i < str.length(); i += chunkSize) {
            int end = Math.min(str.length(), i + chunkSize);
            chunks.add(str.substring(i, end));
        }
        return chunks;
    }
}
