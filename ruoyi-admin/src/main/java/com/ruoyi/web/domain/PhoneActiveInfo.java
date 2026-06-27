package com.ruoyi.web.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
public class PhoneActiveInfo extends BaseEntity {

    // ==================== 数据库物理列（与 Base_Column_List 顺序一致） ====================

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

    @Excel(name = "鸭宝激活状态", readConverterExp = "true=已激活,false=未激活")
    private Boolean activated;

    @Excel(name = "旧手机激活日期")
    private String activateDate;

    @Excel(name = "旧手机保修到期时间")
    private String coverage;

    private String imagePath;   // 上传图片相对路径，多个用逗号分隔

    private String activeInfo;  // 原始查询结果 JSON

    @Excel(name = "查询时系统时间")
    private String sysTime;     // 系统内置时间

    @Excel(name = "协议ID")
    private Long contractId; // 协议id

    private String contractPath; // 协议内容相对路径

    private String contractContent; // 协议内容（富文本）

    @Excel(name = "签名型号")
    private String signatureModel; // 协议签订设备型号

    @Excel(name = "签名IMEI")
    private String signatureImei; // 协议签订设备IMEI

    @Excel(name = "签名日期")
    private String signatureDate; // 协议签订日期

    private String signaturePath; // 手写签名图片相对路径

    /** 创建时间 */
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", required = true)
    private Date createTime;

    /** 创建者（重写父类以加 @Excel） */

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickName;    // 创建人昵称

    /** 更新时间 */
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 更新者（重写父类以加 @Excel） */

    @Excel(name = "跳过API", readConverterExp = "0=否,1=是")
    private Integer skipApiCall; // 是否跳过06 API查询：0-正常查询 1-无旧手机跳过

    @Excel(name = "旧手机状态", readConverterExp = "0=无旧手机,1=丢失/损坏")
    private Integer oldPhoneStatus; // 旧手机状态：0-无旧手机, 1-损坏/丢失

    @Excel(name = "旧手机使用月数")
    private Integer oldPhoneUsageMonths; // 旧手机使用月数

    // ==================== 非数据库列（关联查询 / 前端扩展） ====================

    private String phoneNum; // 留资人电话 ----扩展，不是数据字段，只做关联返回

    private Long infoId; // 留资人id

    private String name; // 留资人姓名 ----扩展，不是数据字段，只做关联返回

    private Integer isSignature; // 是否已签约 ----扩展，不是数据字段，只做关联返回

    private String imageUrl;    // 上传图片完整访问链接，仅用于返回前端

}
