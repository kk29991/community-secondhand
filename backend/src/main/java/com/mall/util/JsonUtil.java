package com.mall.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * JSON 与字符串列表互转 (商品轮播图 / 评价图片 / 标签ID 等字段以 JSON 字符串存库)
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<Long> toLongList(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> result = new java.util.ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // 忽略非法片段
            }
        }
        return result;
    }

    public static String joinLongs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public static String joinStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }
}
