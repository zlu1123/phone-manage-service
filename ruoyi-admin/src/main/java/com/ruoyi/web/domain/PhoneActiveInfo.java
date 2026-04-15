package com.ruoyi.web.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

@Data
public class PhoneActiveInfo extends BaseEntity {

    private Long id;
    private String phoneType;
    private String sn;
    private String imei1;
    private String imei2;
    private String model;
    private Boolean activated;
    private String activateDate;
    private String coverage;
    private String imagePath;   // 上传图片相对路径，多个用逗号分隔
    private String imageUrl;    // 上传图片完整访问链接，仅用于返回前端
    private String activeInfo;  // 原始查询结果 JSON
    private String sysTime;     // 系统内置时间
    private String nickName;    // 创建人昵称
}
