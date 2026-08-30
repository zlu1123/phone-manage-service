package com.ruoyi.web.domain;

import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.util.Date;

/**
 * 订单导入专用模型
 * <p>
 * 说明：
 * <ul>
 *     <li>列名必须与 {@code ExcelUtil.importExcel} 的表头匹配规则一致，
 *         导入模板由 {@code /system/order/importTemplate} 基于本类 @Excel 注解读取列名生成，
 *         修改列名后模板自动同步，无需额外维护。</li>
 *     <li>数值/枚举类字段统一用 String 接收，由 Service 逐行校验并转换，
 *         避免解析阶段因单个单元格类型不符导致整个文件导入失败。</li>
 * </ul>
 */
@Data
public class PhoneActiveInfoImport {

    /** 创建时间（必填），格式 yyyy-MM-dd（可带时分秒） */
    @Excel(name = "创建时间", width = 24, dateFormat = "yyyy-MM-dd", required = true)
    private Date createTime;

    /** 渠道（必填）：填 自有/亚丁（兼容旧模板的 1/0），仅作页面渠道展示直接落库，不参与订单类型判断；订单类型由「旧手机状态」决定 */
    @Excel(name = "渠道", width = 12, required = true)
    private String skipApiCall;

    /** 旧模板表头「跳过API」兼容列：老文件仍可直接导入，与「渠道」列二选一 */
    @Excel(name = "跳过API", width = 12)
    private String legacySkipApiCall;

    /** 旧手机状态：0/无旧手机，1/丢失/损坏（选填；未填默认按无旧手机落库） */
    @Excel(name = "旧手机状态", width = 12)
    private String oldPhoneStatus;

    /** 旧手机序列号（旧手机识别时必填） */
    @Excel(name = "旧手机序列号", width = 20)
    private String sn;

    /** 旧手机品牌 */
    @Excel(name = "旧手机品牌", width = 14)
    private String phoneType;

    /** 旧手机型号 */
    @Excel(name = "旧手机型号", width = 16)
    private String model;

    /** 旧手机IMEI1 */
    @Excel(name = "旧手机IMEI1", width = 18)
    private String imei1;

    /** 旧手机IMEI2 */
    @Excel(name = "旧手机IMEI2", width = 18)
    private String imei2;

    /** 旧手机使用月数 */
    @Excel(name = "旧手机使用月数", width = 14)
    private String oldPhoneUsageMonths;

    /** 店员名称 */
    @Excel(name = "店员名称", width = 12)
    private String nickName;

    /** 所属门店：可填门店名称或门店ID，留空则归属当前操作人所属门店 */
    @Excel(name = "所属门店", width = 16)
    private String store;

    /** 留资人电话：用于匹配留资库，匹配不到时自动创建留资记录 */
    @Excel(name = "留资人电话", width = 16)
    private String phoneNum;

    /** 留资人姓名：留资记录不存在时随电话一起自动创建 */
    @Excel(name = "留资人姓名", width = 12)
    private String name;

    /** 鸭宝激活状态：已激活/未激活（可留空） */
    @Excel(name = "鸭宝激活状态", width = 14)
    private String activated;

    /** 旧手机激活日期 */
    @Excel(name = "旧手机激活日期", width = 14, dateFormat = "yyyy-MM-dd")
    private String activateDate;

    /** 旧手机保修到期时间 */
    @Excel(name = "旧手机保修到期时间", width = 14, dateFormat = "yyyy-MM-dd")
    private String coverage;

    /** 查询时系统时间 */
    @Excel(name = "查询时系统时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private String sysTime;

    /** 签名型号 */
    @Excel(name = "签名型号", width = 16)
    private String signatureModel;

    /** 签名IMEI */
    @Excel(name = "签名IMEI", width = 18)
    private String signatureImei;

    /** 签名日期（选填，未填默认取创建时间） */
    @Excel(name = "签名日期", width = 14, dateFormat = "yyyy-MM-dd")
    private String signatureDate;
}
