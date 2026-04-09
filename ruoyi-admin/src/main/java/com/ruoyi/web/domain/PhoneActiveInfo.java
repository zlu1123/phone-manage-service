package com.ruoyi.web.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import java.util.Date;

@Data
public class PhoneActiveInfo extends BaseEntity {

    private Long id;

    private String sn;
    private String imei1;
    private String imei2;
    private String model;
    private Boolean activated;
    private String activateDate;
    private String coverage;
    private String activeInfo;   // 原始查询结果 JSON
    private String sysTime;       // 系统内置时间
}