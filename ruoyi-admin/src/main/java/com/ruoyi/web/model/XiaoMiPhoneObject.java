package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XiaoMiPhoneObject {

    @JsonProperty("imei")
    private String imei1;
    private String imei2;
    private String model;
    private String storage;
    private Boolean activated;
    private String sn;
    private String activateDate;
    private String coverage;
    private String color;
    private Boolean locked;
    private Integer skuId;
    private String description;
}
