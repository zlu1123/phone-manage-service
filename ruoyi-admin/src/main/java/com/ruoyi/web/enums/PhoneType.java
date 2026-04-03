package com.ruoyi.web.enums;

public enum PhoneType {
    APPLE("1", "苹果", "apple_warranty"),
    XIAOMI("2", "小米/红米", "xiaomi"),
    HUAWEI("3", "华为/荣耀", "huawei");

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
}