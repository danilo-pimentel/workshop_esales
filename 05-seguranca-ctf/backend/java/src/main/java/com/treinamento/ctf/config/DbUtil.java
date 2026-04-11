package com.treinamento.ctf.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DbUtil {

    public static Map<String, Object> lowercaseKeys(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return result;
    }

    public static List<Map<String, Object>> lowercaseKeys(List<Map<String, Object>> list) {
        return list.stream().map(DbUtil::lowercaseKeys).collect(Collectors.toList());
    }
}
