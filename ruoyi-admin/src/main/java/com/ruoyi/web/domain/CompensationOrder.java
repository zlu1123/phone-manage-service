package com.ruoyi.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 赔付订单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompensationOrder {

    /**
     * id
     */
    private Long id;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * 留资人id
     */
    private Long infoId;

    /**
     * 赔付金额
     */
    private Double amount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 驳回原因
     */
    private String rejectionReason;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 修改人
     */
    private String updateBy;
}