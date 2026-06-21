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
    private Long contractId; //协议id
    private String contractPath; //协议内容相对路径
    private String signaturePath; //手写签名图片相对路径
    private String contractContent; //协议内容（富文本）
    private String signatureModel; //协议签订设备型号
    private String signatureImei; //协议签订设备IMEI
    private String signatureDate; //协议签订日期
    private Long infoId; //留资人id

    private String name; // 留资人姓名 ----扩展，不是数据字段，只做关联返回
    private String phoneNum; // 留资人电话 ----扩展，不是数据字段，只做关联返回

    private Integer isSignature; // 是否已签约 ----扩展，不是数据字段，只做关联返回

}
