package com.ruoyi.web.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
public class PhoneActiveInfo extends BaseEntity {

    // ==================== phone_active_info 主表字段（轻量、高频） ====================

    @Excel(name = "ID")
    private Long id;

    @Excel(name = "旧手机序列号", required = true)
    private String sn;

    @Excel(name = "旧手机品牌")
    private String phoneType;

    @Excel(name = "旧手机IMEI1")
    private String imei1;

    @Excel(name = "旧手机IMEI2")
    private String imei2;

    @Excel(name = "旧手机型号")
    private String model;

    @Excel(name = "所属门店")
    private Long storeId;       // 下单时快照，不受店员换店影响

    private Long infoId;        // 留资人id

    @Excel(name = "跳过API", readConverterExp = "0=否,1=是")
    private Integer skipApiCall; // 0-正常查询 1-无旧手机跳过

    @Excel(name = "旧手机状态", readConverterExp = "0=无旧手机,1=丢失/损坏")
    private Integer oldPhoneStatus;

    @Excel(name = "旧手机使用月数")
    private Integer oldPhoneUsageMonths;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", required = true)
    private Date createTime;

    @Excel(name = "昵称")
    private String nickName;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ==================== phone_active_detail 详情表字段（LEFT JOIN） ====================

    @Excel(name = "鸭宝激活状态", readConverterExp = "true=已激活,false=未激活")
    private Boolean activated;

    @Excel(name = "旧手机激活日期")
    private String activateDate;

    @Excel(name = "旧手机保修到期时间")
    private String coverage;

    private String activeInfo;   // 原始查询结果 JSON

    @Excel(name = "查询时系统时间")
    private String sysTime;

    private String imagePath;    // 上传图片相对路径，多个用逗号分隔

    // ==================== phone_order_contract 签约表字段（LEFT JOIN） ====================

    @Excel(name = "协议ID")
    private Long contractId;

    private String contractPath;

    private String contractContent;   // 协议内容（富文本 历史快照）

    @Excel(name = "签名型号")
    private String signatureModel;

    @Excel(name = "签名IMEI")
    private String signatureImei;

    @Excel(name = "签名日期")
    private String signatureDate;

    private String signaturePath;     // 手写签名图片相对路径

    // ==================== 非数据库列（关联查询 / 前端扩展） ====================

    private String storeName;     // 所属门店名称（sys_dept.dept_name）

    private String phoneNum;      // 留资人电话（leave_information.phone_num）

    private String name;          // 留资人姓名（leave_information.name）

    private Integer isSignature;  // 是否已签约（phone_order_contract.signature_imei IS NOT NULL）

    private String imageUrl;      // 上传图片完整访问链接，仅用于返回前端

}
