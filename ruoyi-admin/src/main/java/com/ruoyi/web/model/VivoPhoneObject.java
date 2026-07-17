package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VivoPhoneObject {

    @JsonProperty("imei")
    private String imei1;
    private String imei2;
    private String sn;
    private String model;
    private String color;
    private String storage;
    private String activated;
    private String activateDate;
    private String coverage;
    private String description;
}
