package com.ruoyi.web.domain;

import lombok.Data;

/**
 * 订单签约信息表 phone_order_contract
 *
 * @author ruoyi
 */
@Data
public class PhoneOrderContract {

    /** 订单ID，关联 phone_active_info.id */
    private Long orderId;

    /** 协议ID */
    private Long contractId;

    /** 协议路径 */
    private String contractPath;

    /** 协议内容（富文本历史快照） */
    private String contractContent;

    /** 签名型号 */
    private String signatureModel;

    /** 签名IMEI */
    private String signatureImei;

    /** 签名日期 */
    private String signatureDate;

    /** 手写签名图片相对路径 */
    private String signaturePath;
}
