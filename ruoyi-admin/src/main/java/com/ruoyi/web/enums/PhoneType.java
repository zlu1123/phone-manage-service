package com.ruoyi.web.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum PhoneType {
    APPLE("1", "苹果", "apple_warranty"),
    XIAOMI("2", "小米/红米", "xiaomi"),
    HUAWEI("3", "华为", "huawei"),
    HONOR("4", "荣耀", "honor");

    private final String code;
    private final String name;
    private final String value;

    PhoneType(String code, String name, String value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static String getNameByCode(String code) {
        for (PhoneType type : values()) {
            if (type.code.equals(code)) {
                return type.name;
            }
        }
        return null;
    }

    public static String getValueByCode(String code) {
        for (PhoneType type : values()) {
            if (type.code.equals(code)) {
                return type.value;
            }
        }
        return null;
    }

    public static List<Map<String, String>> toMapList() {
        return Arrays.stream(PhoneType.values())
                .map(type -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", type.getCode());
                    map.put("name", type.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}