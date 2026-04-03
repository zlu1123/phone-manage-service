package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HuaWeiPhoneObject {

    private String imei;
    private String sn;
    private String model;
    private String color;
    private String storage;
    private String description;
    private String img;
    private String brand;
    private String skuItemCode;
    private String product;
    private String productOffering;
    private String productCategoryCode;
    private boolean activated;
    private Type type;
    private boolean hasCare;
    private Purchase purchase;
    private String warrStatus;
    private String covered;
    private String coverage;
    private int daysleft;
    private String productDate;
    private List<Right> right;


    // 内部类 Type
    @Data
    public static class Type {
        private boolean demo;
        private boolean refurbished;
        private boolean retail;
    }

    // 内部类 Purchase
    @Data
    public static class Purchase {
        private String date;
        private String country;
        private String countryName;
    }

    // 内部类 Right
    @Data
    public static class Right {
        private String startDate;
        private String endDate;
        private String code;
        private String name;
        private Integer value;
    }

}
