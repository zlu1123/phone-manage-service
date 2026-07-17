package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OppoPhoneObject {

    @JsonProperty("imei")
    private String imei1;
    private String imei2;
    private String sn;
    private String model;
    private String color;
    private String storage;
    private String coverage;
    private String description;
    private String img;
    private String number;
    private String ram;
    private String rom;
    private String skuCode;
    private String productTypeName;
    private String region;
    private String hasReplacedImei;
    private String extend;
    /** OPPO API 不返回 activated 布尔值，激活信息在 purchase.date 中 */
    private Purchase purchase;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Purchase {
        private String date;
        private String country;
    }
}
