package com.ruoyi.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 赔付订单 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompensationOrderDto {

    /**
     * id
     */
    private Long id;

    /**
     * 订单id
     */
    @NotNull(message = "订单id不能为空")
    private Long orderId;

    /**
     * 留资人id
     */
    @NotNull(message = "留资人id不能为空")
    private Long infoId;

    /**
     * 留资人姓名
     */
    private String name;

    /**
     * 留资人电话
     */
    private String phoneNum;

    /**
     * 签约Imei
     */
    private String signatureImei;

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