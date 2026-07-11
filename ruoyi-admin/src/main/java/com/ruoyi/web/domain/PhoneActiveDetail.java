package com.ruoyi.web.domain;

import lombok.Data;

/**
 * 订单详情表 phone_active_detail
 *
 * @author ruoyi
 */
@Data
public class PhoneActiveDetail {

    /** 订单ID，关联 phone_active_info.id */
    private Long orderId;

    /** 是否激活 */
    private Boolean activated;

    /** 激活日期 */
    private String activateDate;

    /** 保障范围 */
    private String coverage;

    /** 激活信息（第三方原始JSON） */
    private String activeInfo;

    /** 系统时间 */
    private String sysTime;

    /** 图片路径 */
    private String imagePath;
}
