package com.ruoyi.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplePhoneObject {

    private String serial;
    private String model;
    private String thumbnail;
    private Boolean replaced;
    private Boolean registered;
    private Boolean activated;
    private Boolean validPurchaseDate;
    private Integer warrantyDaysRemaining;
    private Boolean acEligible;
    private String loaner;
//    private Boolean pre-activated;
    private Integer warrantyYear;
    private String warrantyStatus;
    private Boolean appleCare;
    private String activeDate;
    private String estPurchaseDate;
    private String repairExpiry;
    private String color;
    private String storage;
    private Boolean appleCareVerifyed;

}
