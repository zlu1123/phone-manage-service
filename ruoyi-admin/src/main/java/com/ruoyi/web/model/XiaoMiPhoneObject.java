package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XiaoMiPhoneObject {

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
    private String skuId;
    private String description;
}
