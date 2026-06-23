package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SamsungPhoneObject {

    private String imei1;
    private String imei2;
    private String serial;
    private String sn;
    private String model;
    private String color;
    private String storage;
    private Boolean activated;
    private String activateDate;
    private String coverage;
    private String description;
    private String warrantyStatus;
}
